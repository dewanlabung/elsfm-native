# Controllers Pattern

Thin controllers that delegate to services and validate via Form Requests.
ELSFM controllers extend `Common\Core\BaseController` and return a JSON
envelope via `$this->success()` / `$this->error()`.

## Anatomy of a Controller

Real example: `app/Http/Controllers/BackstageRequestController.php`

```php
<?php

namespace App\Http\Controllers;

use App\Http\Requests\CrupdateBackstageRequestRequest;
use App\Models\BackstageRequest;
use App\Services\Backstage\CrupdateBackstageRequest;
use Common\Core\BaseController;
use Common\Database\Datasource\Datasource;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

class BackstageRequestController extends BaseController
{
    public function __construct(
        protected BackstageRequest $backstageRequest,
        protected Request $request,
    ) {}

    public function index(): Response
    {
        $userId = $this->request->get('userId');
        $this->authorize('index', [BackstageRequest::class, $userId]);

        $builder = $this->backstageRequest->with(['user', 'artist']);
        $pagination = (new Datasource($builder, $this->request->all()))->paginate();

        return $this->success(['pagination' => $pagination]);
    }

    public function store(CrupdateBackstageRequestRequest $request): Response
    {
        $this->authorize('store', BackstageRequest::class);
        $backstageRequest = app(CrupdateBackstageRequest::class)->execute(
            $request->all(),
        );
        return $this->success(['request' => $backstageRequest]);
    }

    public function destroy($ids): Response
    {
        $this->blockOnDemoSite();
        $this->authorize('destroy', [$backstageRequest = new BackstageRequest(), $ids]);
        
        BackstageRequest::destroy(explode(',', $ids));
        return $this->success();
    }
}
```

## The Rules

1. **Validate at the boundary.** Type-hint a Form Request so validation runs before the method body.
2. **Authorize before doing work.** Call `$this->authorize($ability, $args)` at the top of every action.
3. **No business logic in controllers.** Resolve a service and return its result.
4. **Return the envelope.** Use `$this->success([...])` or `$this->error()`.
5. **Block destructive actions on demo site** with `$this->blockOnDemoSite()`.

## Form Requests

Form Requests live in `app/Http/Requests` and extend
`Common\Core\BaseFormRequest`. Method-aware rules keep create/update in one class:

```php
class CrupdateBackstageRequestRequest extends BaseFormRequest
{
    public function rules(): array
    {
        $required = $this->getMethod() === 'POST' ? 'required' : '';
        return [
            'artist_name' => [$required, 'string', 'min:3'],
            'data' => 'required|array',
        ];
    }
}
```

## Checklist

- [ ] Extends `BaseController`
- [ ] Form Request type-hinted for mutating actions
- [ ] `$this->authorize()` called before work
- [ ] Business logic delegated to a service
- [ ] Returns `$this->success()` / `$this->error()`
- [ ] `$this->blockOnDemoSite()` on destructive actions
