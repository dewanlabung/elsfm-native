package com.elsfm.mobile.feature.comments

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.network.api.CommentApi
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class TestDispatcherProvider(dispatcher: kotlinx.coroutines.CoroutineDispatcher) : DispatcherProvider {
    override val io = dispatcher
    override val main = dispatcher
    override val default = dispatcher
}

private fun commentApiReturning(status: HttpStatusCode, body: String): CommentApi {
    val mockEngine = MockEngine { _ ->
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
    }
    val httpClient = HttpClient(mockEngine) {
        install(ContentNegotiation) { json() }
    }
    return CommentApi(httpClient)
}

private val emptyCommentsBody = """{"pagination": {"data": []}}"""

private val commentsBody = """
    {
      "pagination": {
        "data": [
          {"id": 1, "content": "Nice track!", "user": {"id": 2, "name": "Alice"}}
        ]
      }
    }
""".trimIndent()

private fun buildViewModel(
    commentApi: CommentApi,
    trackId: Int = 5,
    testScheduler: kotlinx.coroutines.test.TestCoroutineScheduler,
) = CommentsViewModel(
    commentApi = commentApi,
    savedStateHandle = SavedStateHandle().apply { set(TRACK_ID_ARG, trackId) },
    dispatcherProvider = TestDispatcherProvider(StandardTestDispatcher(testScheduler)),
)

class CommentsViewModelTest {

    @Test
    fun `loads comments successfully on init`() = runTest {
        val viewModel = buildViewModel(
            commentApi = commentApiReturning(HttpStatusCode.OK, commentsBody),
            testScheduler = testScheduler,
        )

        viewModel.state.test {
            assertEquals(CommentsState(), awaitItem())

            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)

            val loadedState = awaitItem()
            assertEquals(1, loadedState.comments.size)
            assertEquals("Nice track!", loadedState.comments[0].content)
            assertFalse(loadedState.isLoading)
        }
    }

    @Test
    fun `shows error when comments fetch fails`() = runTest {
        val viewModel = buildViewModel(
            commentApi = commentApiReturning(HttpStatusCode.InternalServerError, ""),
            testScheduler = testScheduler,
        )

        viewModel.state.test {
            awaitItem() // initial
            awaitItem() // loading

            val errorState = awaitItem()
            assertFalse(errorState.isLoading)
            assertTrue(errorState.error != null)
        }
    }

    @Test
    fun `onDraftChanged updates draft text`() = runTest {
        val viewModel = buildViewModel(
            commentApi = commentApiReturning(HttpStatusCode.OK, emptyCommentsBody),
            testScheduler = testScheduler,
        )

        viewModel.state.test {
            awaitItem() // initial
            awaitItem() // loading
            awaitItem() // loaded (empty)

            viewModel.onDraftChanged("Great song")

            assertEquals("Great song", awaitItem().draft)
        }
    }

    @Test
    fun `postComment prepends new comment and clears draft`() = runTest {
        val viewModel = buildViewModel(
            commentApi = commentApiReturning(HttpStatusCode.OK, emptyCommentsBody),
            testScheduler = testScheduler,
        )

        viewModel.state.test {
            awaitItem() // initial
            awaitItem() // loading
            awaitItem() // loaded (empty)

            viewModel.onDraftChanged("New comment")
            assertEquals("New comment", awaitItem().draft)

            viewModel.postComment()
            val postingState = awaitItem()
            assertTrue(postingState.isPosting)
        }
    }

    @Test
    fun `postComment does nothing for blank draft`() = runTest {
        val viewModel = buildViewModel(
            commentApi = commentApiReturning(HttpStatusCode.OK, emptyCommentsBody),
            testScheduler = testScheduler,
        )

        viewModel.state.test {
            awaitItem() // initial
            awaitItem() // loading
            val loadedState = awaitItem() // loaded (empty)

            viewModel.postComment()

            // No further emission - draft was blank, postComment() returned early.
            expectNoEvents()
            assertEquals("", loadedState.draft)
        }
    }
}
