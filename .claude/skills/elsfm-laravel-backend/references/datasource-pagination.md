# Datasource: Filtering & Pagination

`Common\Database\Datasource\Datasource`
(`common/foundation/src/Database/Datasource/Datasource.php`) is BeMusic's unified
filtering + sorting + pagination layer. **It replaces Laravel API Resources in this
codebase** — controllers do not build `JsonResource` collections; they hand a query
builder plus the raw request params to a `Datasource` and return its paginator.

## Basic usage

```php
use Common\Database\Datasource\Datasource;

$builder = BackstageRequest::with(['user', 'artist']);

$pagination = (new Datasource($builder, $request->all()))->paginate();

return $this->success(['pagination' => $pagination]);
```

`$request->all()` is passed straight through. Datasource camel-cases every param
key internally, so `per_page` and `perPage` both work.

## Constructor

```php
new Datasource(
    $model,                       // Eloquent model, Builder, or Relation
    array $params = [],           // usually $request->all()
    DatasourceFilters $filters = null,
    string|null $filtererName = 'mysql',  // or a Scout driver name
    bool $qualifySortColumns = true,
);
```

## Supported request params

| Param | Effect |
|-------|--------|
| `page` | Page number (default 1) |
| `perPage` | Page size; falls back to builder `limit`, else 15 |
| `with` | Comma-separated relations to eager load (`with=user,artist`) |
| `withCount` | Comma-separated relation counts |
| `query` | Search term (routes to Scout filterer if model is `Searchable`) |
| `order` | `column\|dir` or `column:dir` (e.g. `created_at\|desc`) |
| `orderBy` / `orderDir` | Alternative split-param ordering |
| `filters` | Encoded filter set consumed by `DatasourceFilters` |
| `paginate` | `simple` (default), `lengthAware`, or `preferLengthAware` |

## Two output methods

```php
(new Datasource($builder, $params))->paginate(); // AbstractPaginator (paged)
(new Datasource($builder, $params))->get();       // Collection (limited, no paging)
```

## How ordering resolves

`buildQuery()` resolves an order column then checks the model for a matching scope
before falling back to a raw `orderBy`:

- If `orderBy<Col>()` method or `scopeOrderBy<Col>()` scope exists on the model,
  it's invoked (e.g. `orderByPopularity` on `User`).
- Otherwise it does `orderBy(snake_case(col), dir)`.
- If the query has an `AS relevance` column (mysql fulltext), it orders by relevance.
- Default is `updated_at desc`.

Set `$datasource->secondaryOrderCol = 'id'` when the primary sort column is not
unique, to keep pagination stable.

## Search backends

If the model uses `Laravel\Scout\Searchable` and a `query` param is present,
Datasource dispatches to the matching filterer (`MysqlFilterer`,
`MeilisearchFilterer`, `TntFilterer`, `AlgoliaFilterer`, `ElasticFilterer`).
Otherwise it uses `MysqlFilterer`. Allowed filter columns come from the model's
static `filterableFields()`.

## Overriding the final query

```php
$datasource = new Datasource($builder, $request->all());
$datasource->setQueryCallback(function ($builder) {
    $builder->where('public', true);
});
$pagination = $datasource->paginate();
```

To disable ordering entirely set `$datasource->order = false;` before `paginate()`.

## Why not API Resources

BeMusic models implement their own serialization (`toArray()`,
`toNormalizedArray()`, hidden/appends arrays on the base models), so there is no
separate Resource layer. Shaping happens on the model; selection/filtering/paging
happens in Datasource. Adding a `JsonResource` would duplicate logic the base
models already own — don't introduce one.

## Controller response envelope

Pair Datasource with `BaseController::success()`:

```php
return $this->success(['pagination' => $pagination]);
// -> { "pagination": {...}, "status": "success" }
```
