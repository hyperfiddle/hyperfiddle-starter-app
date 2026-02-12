# HFQL Technical Reference

HFQL (HyperFiddle Query Language) is a compositional query language for building navigable data explorers. It combines pull-syntax, entity protocols, and a sitemap to generate full CRUD navigators from declarative specifications.

## Core Concepts

### The `hfql` Macro

The `hfql` macro defines a query tree. It supports several forms:

```clojure
;; Scalar: navigate a single key
(hfql :a)                        ;; => looks up :a on the entity

;; Pull: select multiple keys from an entity
(hfql [:a :b :c])                ;; => returns map {:a v, :b v, :c v}

;; Traversal: call a function, then pull from each result
(hfql {(my-fn) {* [:name :id]}}) ;; => calls (my-fn), fans out over results

;; Seeded: provide an entity to query
(hfql [:a :b] my-entity)        ;; => two-arg form, seeds the entity
```

Column expressions in a pull can be:
- **Keywords**: `:address`, `:name` — looked up on the entity via `get`/ILookup
- **Symbols (functions)**: `my-fn` — called with the entity as argument
- **Java methods**: `.getName`, `.getId` — called on the entity
- **Lambdas**: `#(.getName %)` — called with entity (but damages column label — renders as "(fn anonymous ...)")
- **`*` (wildcard)**: includes all keys of the entity (like `SELECT *`)

### Seeding

`(hfql [...] entity)` seeds the query with a root entity:
```clojure
(hfql [:user/name :user/email] {:user/name "Alice" :user/email "alice@ex.com"})
```

Internally: `(seed {'% (right entity)} (hfql [...]))`

The `%` symbol in the scope represents "the current entity at this point in the tree."

### Traversals and Fan-Out (`*`)

```clojure
(hfql {(all-users) {* [:name :email]}})
```

- `(all-users)` is called to produce a collection
- `*` fans out: one row per element
- `[:name :email]` is the pull expression applied to each element

The `*` can carry metadata:
- `^{::hfql/select \`find-user}` — when a row is selected, navigate to `find-user`

## Protocols

### `Identifiable`
```clojure
(defprotocol Identifiable
  :extend-via-metadata true
  (-identify [o]))
```

Returns a **symbolic sexpr** that uniquely identifies the entity and can be serialized into a URL. The sexpr must be resolvable by `-hfql-resolve`.

```clojure
(extend-type Owner
  Identifiable (-identify [o] `(find-owner ~(.getId o))))
;; produces: (my.ns/find-owner 42)
```

For Clojure records with string IDs:
```clojure
(extend-type AccountRef
  Identifiable (-identify [r] `(find-account ~(:address r))))
;; produces: (my.ns/find-account "3wK...")
```

**Key behavior**: `Identifiable` provides the identity sexpr used for URL routing and link construction. It does NOT automatically create clickable links — links require explicit `::hfql/link` metadata or named accessors in the sitemap.

### `Suggestable`
```clojure
(defprotocol Suggestable
  :extend-via-metadata true
  (-suggest [o]))
```

Returns an `hfql` form that defines the default columns to display when viewing this entity type. Used for:
- Column auto-suggestion when `*` or `...` is used
- Inline rendering of entity values in table cells

```clojure
(extend-type TokenAccount
  Suggestable (-suggest [_] (hfql [:address :owner :mint :balance])))
```

For records, if no `Suggestable` is explicitly defined, HFQL auto-suggests all record fields via `suggest-record`.

### `-hfql-resolve` (Multimethod)
```clojure
(defmulti -hfql-resolve -resolve-dispatch)
```

Dispatches on `(first sexpr)` for sequences. Used to hydrate entities from URL-encoded sexprs.

```clojure
(defmethod -hfql-resolve `find-account [[_ addr]] (find-account addr))
(defmethod -hfql-resolve `find-tx [[_ sig]] (find-tx sig))
```

The resolver receives the full sexpr (e.g., `[find-account "3wK..."]`) and must return the entity.

### `ComparableRepresentation`
```clojure
(defprotocol ComparableRepresentation
  (-comparable [this]))
```

Returns a representation suitable for `compare`. Used for sorting in tables. Strings, keywords, and symbols have built-in implementations.

### `Datafiable`
```clojure
(defprotocol Datafiable
  :extend-via-metadata true
  (-datafy [o]))
```

Teaches HFQL how to handle foreign objects (iterators, streams, etc.). Not to be confused with `clojure.core.protocols/Datafiable` — this is HFQL's own protocol. Used for mutable/forward-only objects that can't be treated as regular seqs.

### `Navigable`
```clojure
(defprotocol Navigable
  :extend-via-metadata true
  (-nav [coll k v]))
```

Same semantics as `clojure.core.protocols/Navigable`. Defaults to delegating to `ccp/Navigable`. Controls how HFQL traverses entity relationships.

## Sitemap

A sitemap is a map of `fully-qualified-symbol -> hfql-form`. It defines all the "pages" of the navigator.

```clojure
(def sitemap
  {`all-owners      (hfql {(all-owners) {* ^{::hfql/select `find-owner}
                                          [^{::hfql/link `(find-owner ~'%)} owner-name
                                           :city :telephone]}})
   `find-owner      (hfql [.getFirstName .getLastName .getCity .getTelephone])
   `find-pet        (hfql [.getName .getBirthDate {.getType [.getName]}])})
```

### Sitemap entry types

1. **List view** (traversal with fan-out):
   ```clojure
   `my-list (hfql {(list-fn) {* ^{::hfql/select `detail-page}
                               [^{::hfql/link `(detail-page ~'%)} first-col
                                :col2 :col3]}})
   ```

2. **Detail view** (pull):
   ```clojure
   `detail-page (hfql [:field1 :field2 :field3])
   ```

## Link Mechanism

### `::hfql/link` — Explicit links on columns

Metadata attached to the first column in a list view to make it clickable:

```clojure
^{::hfql/link `(find-account ~'%)} holder-address
```

**Important**: Keywords don't implement `IObj` and can't carry metadata. Use named `defn` accessors for link columns.

### How link-href works (navigator6)

In `hyperfiddle.navigator6.rendering/link-href`:

```clojure
(defn- link-href [object edgeT value]
  (when-let [symbolic-link-href (unquote* (::hfql/link (hfql/metas edgeT)))]
    (let [denv {'%  (hfql/identify object)
                (hfql/symbolic-edge edgeT) (hfql/identify value)
                '%v (or (hfql/identify value) value)}]
      (template-link denv symbolic-link-href))))
```

The `denv` (dynamic environment) provides three substitution keys:

| Key | Value | Description |
|-----|-------|-------------|
| `%` | `(hfql/identify object)` | Identity sexpr of the **row entity** (e.g., `(find-account "3wK...")`) |
| `(symbolic-edge)` | `(hfql/identify value)` | Identity of the **column value** (nil if value isn't Identifiable) |
| `%v` | `(or (identify value) value)` | Identity of column value, or raw value if not Identifiable |

`template-link` uses `clojure.core/replace` to substitute symbols in the template. For collections (lists), each element matching a denv key is replaced.

### `%` vs `%v` in link templates

The `denv` maps `%` to `(hfql/identify object)` — a full sexpr like `(find-account "3wK...")`. Despite this, `~'%` is the standard pattern and works correctly in practice:

```clojure
;; Standard pattern — works:
^{::hfql/link `(find-account ~'%)} holder-address
```

This is the pattern used throughout PetClinic and Solana navigator examples. The `template-link` function uses `clojure.core/replace` on the link sexpr list, and the navigator/router correctly resolves the resulting form.

The `%v` key is also available: `(or (hfql/identify value) value)`. It resolves to the identity of the **column value** (via `hfql/identify`), or falls back to the raw value if not `Identifiable`. Use `%v` when you specifically need the column value rather than the row entity identity.

### `::hfql/select` — Row selection target

Metadata on `*` that tells the navigator which sitemap page to navigate to when a row is selected:

```clojure
{* ^{::hfql/select `find-account} [...columns...]}
```

When a row is clicked, the navigator uses `-identify` on the row entity to construct the URL, then navigates to the `find-account` sitemap entry.

### All links require `::hfql/link`

There is no auto-linking. Every clickable link in the navigator — whether in list views or detail views — must be declared via `::hfql/link` metadata on a named accessor symbol. `Identifiable` provides the identity sexpr for URL routing, but does not by itself create links.

## Pretty Printing / Cell Rendering

HFQL renders cell values through `pretty-printer-dispatch`:
- **Bounded-size values** (nil, boolean, char, keyword, symbol, fixed-size numbers): rendered via `pr-str`
- **Strings**: truncated to 100 chars, rendered via `pr-str`
- **Collections**: inline rendering with item count and truncation
- **User-registered handlers**: via `*server-pretty` binding — a map of `type -> render-fn`

Custom renderers can be attached per-column via `::hfql/Render` metadata.

## The Ref Pattern (AccountRef, TxRef, etc.)

For data models with string foreign keys (like Solana addresses or transaction signatures), create wrapper records — one per entity type:

```clojure
;; In the data layer:
(defrecord AccountRef [address])
(defrecord TxRef [signature])
```

### Foreign-key wrapping in constructors

Wrap foreign-key fields when constructing entities so they auto-link in detail views:

```clojure
(->TokenAccount
  address                            ;; PK — leave as raw string
  (->AccountRef owner-address)       ;; FK → auto-links to find-account
  (->AccountRef mint-address)        ;; FK → auto-links to find-account
  balance ...)

(->TxDetail
  signature                          ;; PK — leave as raw string
  block-time slot fee status
  (->AccountRef from-address)        ;; FK → auto-links to find-account
  (->AccountRef to-address)          ;; FK → auto-links to find-account
  amount ui-amount num-instructions)
```

### Protocol extensions for Ref types

```clojure
;; In the navigator layer:
(extend-type AccountRef
  Identifiable (-identify [r] `(find-account ~(:address r)))
  Suggestable  (-suggest [_] (hfql [:address])))

(extend-type TxRef
  Identifiable (-identify [r] `(find-tx ~(:signature r)))
  Suggestable  (-suggest [_] (hfql [:signature])))
```

**Result**: Every field containing an `AccountRef` automatically renders as a clickable link navigating to `find-account`. Every `TxRef` auto-links to `find-tx`. No `::hfql/link` metadata needed in detail views.

### PK fields: don't wrap in constructors, use named accessors instead

Don't wrap an entity's own primary key in a Ref record at the constructor level — it would create a circular self-link and break `-identify` (which extracts the raw PK from the field).

Instead, use a **named accessor** in the sitemap that wraps the PK on the fly:

```clojure
;; Named accessors that return Ref types:
(defn account-address [a] (sol/->AccountRef (:address a)))
(defn tx-signature [tx] (sol/->TxRef (:signature tx)))

;; Use in detail view sitemap entries:
(def sitemap
  {`find-account (hfql [account-address :owner :mint :balance :ui-balance :sol-balance])
   `find-tx      (hfql [tx-signature :block-time :slot :fee :status
                         :from-address :to-address :amount :ui-amount :num-instructions])})
```

The accessor `account-address` returns an `AccountRef`, which implements `Identifiable`, so HFQL auto-renders it as a clickable link. The underlying record field `:address` stays a raw string, so `-identify` still works correctly.

This pattern cleanly separates concerns: the data layer stores raw values, the navigator layer adds navigation behavior via accessors.

## Complete Navigator Pattern

### 1. Data layer (`my_data.clj`)
```clojure
(defrecord MyEntity [id name ref-id])
(defrecord EntityRef [id])

(defn list-entities [] ...)
(defn find-entity [id] ...)
```

### 2. Navigator layer (`nav_my_data.clj`)
```clojure
;; Helper functions (thin wrappers)
(defn all-entities [] (data/list-entities))
(defn find-entity [id] (data/find-entity id))

;; Named accessor for list view link column
(defn entity-name [e] (:name e))

;; Named PK accessor for detail view — returns Ref to enable auto-linking
(defn entity-id [e] (data/->EntityRef (:id e)))

;; Protocol extensions
(extend-type MyEntity
  Identifiable (-identify [e] `(find-entity ~(:id e)))
  Suggestable  (-suggest [_] (hfql [:id :name :ref-id])))

(extend-type EntityRef
  Identifiable (-identify [r] `(find-entity ~(:id r)))
  Suggestable  (-suggest [_] (hfql [:id])))

;; Sitemap
(def sitemap
  {`all-entities (hfql {(all-entities) {* ^{::hfql/select `find-entity}
                                         [^{::hfql/link `(find-entity ~'%)} entity-name
                                          :ref-id]}})
   `find-entity  (hfql [entity-id :name :ref-id])})

;; Resolvers
(defmethod -hfql-resolve `find-entity [[_ id]] (find-entity id))
```

### 3. Entry point (`demo.cljc`)
```clojure
(require '[hyperfiddle.navigator6 :refer [HfqlRoot]])

(def index ['my.ns/all-entities])

(HfqlRoot (merge my-nav/sitemap other-nav/sitemap) index)
```

## HFQL Macro Internals

The `hfql` macro pipeline:
1. **Analyze** (`ana/analyze`): Parses the form into a rose tree AST. Metadata (including `::hfql/link`, `::hfql/select`) is preserved.
2. **Marshal** (`marshal`): Emits the AST as quotable Clojure data via `emit*`/`escape`.
3. **Emit** (code generation): Produces Clojure code that reconstructs the AST at runtime.
4. **Unmarshal** (`unmarshal`): At runtime, `unquote*` strips quote wrappers from embedded forms.
5. **Unfold** (`unfoldT hfqlT`): Builds the cofree comonad tree that drives navigation.

Key internal types:
- **CofreeT**: The core tree structure. Each node has metadata (metas), a scope (the entity in context), and children (next-steps).
- **scope**: `{'% entity}` — the current entity at each point.
- **metas**: Analysis metadata including `::ana/form`, `::ana/type`, `::ana/multiplicity`, and user metadata.

### Navigation functions
```clojure
(nav stepT)         ;; Force-evaluate and get the value at this point
(nav* stepT)        ;; Get the Either-wrapped value
(next-steps stepT)  ;; Get child points
(metas stepT)       ;; Get metadata at this point
(scope stepT)       ;; Get the scope (entity map) at this point
(values stepT)      ;; Pre-order traversal of all values
(pull stepT)        ;; Pull the full result as nested maps
(seed scope stepT)  ;; Seed a point with a new scope
```

## Keyboard shortcuts in the Navigator UI

- Arrow keys navigate the grid
- Enter selects a row (navigates to detail via `::hfql/select`)
- Clicking a link column navigates via `::hfql/link`

## Data Source Patterns

HFQL can browse any data source that produces Clojure records/maps. The data layer is responsible for fetching and shaping data; the navigator layer adds protocols and sitemap. Several patterns are used across the demo apps:

### Shell-out pattern (Docker, macOS, Git, Network, GitHub)

For CLI tools that emit structured output:

```clojure
(require '[clojure.java.shell :as sh]
         '[clojure.data.json :as json])

;; Shell helper — run command, return stdout or nil
(defn- my-cmd [& args]
  (try
    (let [{:keys [exit out]} (apply sh/sh "my-tool" args)]
      (when (zero? exit) out))
    (catch Exception _ nil)))

;; Parse JSON lines (e.g., `docker ps --format '{{json .}}'`)
(defn- parse-json-lines [s]
  (->> (str/split-lines s)
    (remove str/blank?)
    (mapv #(json/read-str % :key-fn keyword))))

;; Data function
(defn containers []
  (when-let [out (my-cmd "ps" "-a" "--format" "{{json .}}")]
    (mapv (fn [c] (->MyRecord (:ID c) (:Name c) ...))
      (parse-json-lines out))))
```

Used by: `nav_docker.clj` (Docker CLI), `nav_macos.clj` (`system_profiler -json`), `nav_network.clj` (`lsof -i`), `nav_git_explorer.clj` (`git` CLI), `nav_github.clj` (`gh` CLI).

### HTTP API pattern (Solana, Wikipedia)

For REST/JSON-RPC APIs using Java's built-in `HttpClient`:

```clojure
(import '[java.net URI]
        '[java.net.http HttpClient HttpRequest HttpResponse$BodyHandlers])

(defonce ^:private http-client
  (-> (HttpClient/newBuilder)
    (.connectTimeout (Duration/ofSeconds 10))
    (.build)))

(defn- api-get [path]
  (let [req (-> (HttpRequest/newBuilder)
              (.uri (URI/create (str base-url path)))
              (.header "Accept" "application/json")
              (.build))
        resp (.send http-client req (HttpResponse$BodyHandlers/ofString))]
    (when (= 200 (.statusCode resp))
      (json/read-str (.body resp) :key-fn keyword))))
```

Used by: `solana_rpc.clj` (Solana JSON-RPC), `nav_wikipedia.clj` (Wikipedia REST API).

### JVM introspection pattern (JVM Deep Diagnostics)

For browsing JVM internal state via `java.lang.management`:

```clojure
(import '[java.lang.management ManagementFactory ThreadMXBean])

(defn all-threads []
  (let [mx (ManagementFactory/getThreadMXBean)
        ids (.getAllThreadIds mx)
        infos (.getThreadInfo mx ids 0)]
    (mapv (fn [ti]
            (->JvmThread (.getThreadId ti) (.getThreadName ti)
              (str (.getThreadState ti)) ...))
      (remove nil? infos))))
```

Used by: `nav_jvm_deep.clj` (threads, memory pools, GC, deadlock detection, class loading).

### Spring Boot JPA pattern (PetClinic, E-commerce)

For navigating Spring JPA entities using Java interop:

```clojure
;; Boot the Spring context from Clojure
(defonce ^:private *context (atom nil))
(defn context [] (or @*context (reset! *context (SpringApplication/run MyApp (into-array String [])))))
(defn owner-repo ^OwnerRepository [] (.getBean (context) OwnerRepository))

;; Suggestable with Java interop methods
(extend-type Owner
  Suggestable (-suggest [_] (hfql [.getFirstName .getLastName {.getPets {* ...}}])))
```

Used by: `nav_spring_petclinic.clj`, `nav_spring_ecommerce.clj`.

## Merging Multiple Sitemaps

Combine navigators into a mega-demo by merging sitemaps:

```clojure
(def sitemap
  (merge
    petclinic/sitemap
    ecommerce/sitemap
    solana/sitemap
    docker/sitemap
    jvm-deep/sitemap
    macos/sitemap
    network/sitemap
    git-explorer/sitemap
    wikipedia/sitemap
    github/sitemap))
```

Each nav module is self-contained: records, data functions, protocol extensions, sitemap, and resolvers. Since protocol extensions and `defmethod` resolvers are global, simply requiring the namespace makes them available. The sitemaps are plain maps that merge cleanly as long as keys (fully-qualified symbols) don't collide.

**Important**: Merge sitemaps inside `e/server` at render time (not in a top-level `def`) to preserve Electric hot reload:

```clojure
(let [sitemap (e/server (merge nav-a/sitemap nav-b/sitemap ...))]
  (HfqlRoot sitemap index))
```

## Example Navigator Index

All navigators live in `electric-fiddle/src/dustingetz/`:

| Module | Data Source | Key entities | Navigation pattern |
|--------|-----------|-------------|-------------------|
| `nav_solana_usdc` | Solana JSON-RPC | UsdcHolder, TokenAccount, SolTx, TxDetail | holders → accounts ↔ transactions |
| `nav_spring_petclinic` | Spring JPA | Owner, Pet, Vet, Visit | owners → pets → visits, vets → specialties |
| `nav_spring_ecommerce` | Spring JPA | Customer, Product, Order, Review, Category | customers → orders → items → products |
| `nav_docker` | `docker` CLI | Container, Image, Volume, Network | containers ↔ images ↔ networks |
| `nav_jvm_deep` | JVM MXBeans | Thread, MemoryPool, Gc, Runtime, OS | threads → locks/deadlocks, memory pools, GC stats |
| `nav_macos` | `system_profiler` | Hardware, NetworkIface, UsbDevice, Display, Storage | hardware info, interfaces → detail, volumes → detail |
| `nav_network` | `lsof -i` | Connection, Listener, Process | connections → process → all its connections |
| `nav_git_explorer` | `git` CLI | Branch, Commit, FileChange, Tag, Stash | branches → commits → files, tags → commits |
| `nav_wikipedia` | Wikipedia REST API | Article, Link, Category, Search | articles ↔ links ↔ categories → members (infinite) |
| `nav_github` | `gh` CLI | Repo, Issue, PR, User, Release, WorkflowRun | repos → issues/PRs → authors → repos, releases, actions |
| `nav_git` | JGit | Git, RevCommit, Ref, PersonIdent | repo → branches → commits, refs → log |
| `nav_jvm` | JVM MXBeans | ThreadMXBean, MemoryMXBean, ThreadInfo | (original lightweight JVM browser) |
| `nav_aws` | Cognitect AWS SDK | S3 client, buckets, objects | S3 → buckets → objects |
| `nav_hn` | Hacker News API | Stories, Items, Comments | stories → items → kids |

## Caching Considerations

For data sources with rate limits (like Solana RPC's 100 req/10s), implement TTL caching in the data layer:
- Short TTL (30s) for list data that changes frequently
- Long TTL (5min+) for immutable data (e.g., committed transactions)
- Use `defonce` for singleton resources (HTTP clients, cache atoms)

For shell-based data sources (Docker, Git, lsof), caching is optional since the local CLI is fast. But for external HTTP APIs (Wikipedia, Solana), a TTL cache prevents redundant network calls:

```clojure
(defonce ^:private cache (atom {}))

(defn- cache-get [k ttl-ms]
  (when-let [[v ts] (get @cache k)]
    (when (< (- (System/currentTimeMillis) ts) ttl-ms) v)))

(defn- cache-put! [k v]
  (swap! cache assoc k [v (System/currentTimeMillis)]) v)
```

## Common Pitfalls

1. **Keywords can't carry metadata**: `^{::hfql/link ...} :my-keyword` silently fails. Use named `defn` symbols for link columns.

2. **Anonymous lambdas damage column labels**: `#(.getName %)` renders as "(fn anonymous ...)". Use named `defn` accessors instead.

3. **Bare `%` in hfql map keys**: `{(my-fn (:field %)) [...]}` fails with "Unable to resolve symbol: %". The `hfql` macro doesn't walk map keys to replace `%`. Only works inside `#(...)` where the Clojure reader handles it.

4. **`%` vs `%v` in link templates**: `%` resolves to the row entity's identity sexpr. `%v` resolves to the column value's identity (or the raw value). Both work — `~'%` is the standard pattern for link columns that navigate to the row's detail page.

5. **Entity PK fields as Ref in constructors**: Don't wrap an entity's own primary key in a Ref record at the constructor level — `-identify` extracts the raw PK and wrapping it would break that. Instead, use a named accessor in the sitemap that wraps the PK on the fly (see "PK fields" section above).

6. **Records vs maps**: Both work with keyword field access in `hfql` forms. Records additionally support protocol extension (`Identifiable`, `Suggestable`) and auto-suggest via `suggest-record`.
