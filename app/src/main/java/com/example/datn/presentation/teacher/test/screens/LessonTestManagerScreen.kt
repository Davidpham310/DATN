package com.example.datn.presentation.teacher.test.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.datn.domain.models.Test
import com.example.datn.presentation.teacher.test.LessonTestManagerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonTestManagerScreen(
    lessonId: String,
    lessonTitle: String,
    onNavigateToQuestions: (testId: String, testTitle: String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: LessonTestManagerViewModel = hiltViewModel()
) {
    val state = viewModel.state

    LaunchedEffect(lessonId) {
        viewModel.setLesson(lessonId, lessonTitle)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TopAppBar(title = { Text("Bài kiểm tra: $lessonTitle") })
        Text("Danh sách bài kiểm tra", style = MaterialTheme.typography.titleMedium)

        LazyColumn {
            items(state.value.tests, key = { it.id }) { test ->
                TestItem(test = test, onClick = { onNavigateToQuestions(test.id, test.title) })
            }
        }
    }
}

@Composable
private fun TestItem(test: Test, onClick: () -> Unit) {
    Surface(tonalElevation = 1.dp, modifier = Modifier
        .fillMaxWidth()
        .clickable { onClick() }) {
        Column(Modifier.padding(12.dp)) {
            Text(test.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!test.description.isNullOrBlank()) {
                Spacer(Modifier.padding(top = 4.dp))
                Text(test.description ?: "", style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
    Spacer(Modifier.padding(top = 8.dp))
}


