package com.freetime.news

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.freetime.news.ui.theme.FreetimeNewsTheme
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.noties.markwon.Markwon
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.text.DateFormat
import java.util.Date

private val blogApi: BlogApi by lazy {
    val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    Retrofit.Builder()
        .baseUrl("https://api.free-time.me/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(BlogApi::class.java)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FreetimeNewsTheme {
                FreetimeNewsApp()
            }
        }
    }
}

@Composable
private fun FreetimeNewsApp() {
    var selectedSlug by remember { mutableStateOf<String?>(null) }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        if (selectedSlug == null) {
            PostListScreen(
                modifier = Modifier.padding(innerPadding),
                onPostClick = { selectedSlug = it }
            )
        } else {
            PostDetailScreen(
                slug = selectedSlug!!,
                modifier = Modifier.padding(innerPadding),
                onBack = { selectedSlug = null }
            )
        }
    }
}

@Composable
private fun PostListScreen(
    modifier: Modifier = Modifier,
    onPostClick: (String) -> Unit
) {
    var posts by remember { mutableStateOf<List<BlogPostSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(reloadKey) {
        loading = true
        error = null
        try {
            posts = blogApi.getPosts().posts
        } catch (e: Exception) {
            error = e.message ?: "Could not load news."
        } finally {
            loading = false
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Freetime News",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        when {
            loading -> CircularProgressIndicator()
            error != null -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
                Button(onClick = { reloadKey++ }) { Text("Retry") }
            }
            posts.isEmpty() -> Text("No news available.")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(posts, key = { it.slug }) { post ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPostClick(post.slug) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(post.title, style = MaterialTheme.typography.titleLarge)
                            if (post.releaseTimestamp > 0) {
                                Text(
                                    formatTimestamp(post.releaseTimestamp),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            if (post.categories.isNotEmpty()) {
                                Text(
                                    post.categories.joinToString(" • "),
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PostDetailScreen(
    slug: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    var post by remember(slug) { mutableStateOf<BlogPost?>(null) }
    var error by remember(slug) { mutableStateOf<String?>(null) }

    LaunchedEffect(slug) {
        try {
            post = blogApi.getPost(slug)
        } catch (e: Exception) {
            error = e.message ?: "Could not load this article."
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onBack) { Text("Back") }
        }

        when {
            error != null -> Text(
                error!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp)
            )
            post == null -> CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
            else -> {
                val article = post!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(article.title, style = MaterialTheme.typography.headlineMedium)
                    }
                    if (article.releaseTimestamp > 0) {
                        item { Text(formatTimestamp(article.releaseTimestamp)) }
                    }
                    if (article.categories.isNotEmpty()) {
                        item { Text(article.categories.joinToString(" • ")) }
                    }
                    item {
                        MarkdownContent(prepareArticleMarkdown(article.markdown))
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownContent(markdown: String) {
    val textColor = MaterialTheme.colorScheme.onBackground.toArgb()
    val linkColor = MaterialTheme.colorScheme.primary.toArgb()

    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            TextView(context).apply {
                setTextColor(textColor)
                setLinkTextColor(linkColor)
                textSize = 17f
                movementMethod = LinkMovementMethod.getInstance()
                linksClickable = true
                setLineSpacing(0f, 1.12f)
            }
        },
        update = { textView ->
            textView.setTextColor(textColor)
            textView.setLinkTextColor(linkColor)
            val markwon = Markwon.create(textView.context)
            markwon.setMarkdown(textView, markdown)
        }
    )
}

private fun prepareArticleMarkdown(markdown: String): String {
    return markdown
        .lineSequence()
        .filterNot { line ->
            line.matches(Regex("^#\\s+.+$")) ||
                line.contains("Released on", ignoreCase = true) ||
                line.contains("Categories:", ignoreCase = true)
        }
        .joinToString("\n")
        .replace(Regex("<t:(\\d+)(?::[tTdDfFR])?>")) { match ->
            formatTimestamp(match.groupValues[1].toLong())
        }
        .trim()
}

private fun formatTimestamp(unixSeconds: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .format(Date(unixSeconds * 1000))
