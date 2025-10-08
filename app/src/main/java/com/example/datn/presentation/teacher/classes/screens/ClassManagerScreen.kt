//package com.example.datn.presentation.teacher.classes.screens
//
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.*
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Add
//import androidx.compose.material.icons.filled.ArrowBack
//import androidx.compose.material3.Card
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.collectAsState
//import androidx.compose.runtime.getValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.dp
//import androidx.lifecycle.viewmodel.compose.viewModel
//import com.example.datn.R
//import com.example.datn.presentation.teacher.classes.ClassManagerViewModel
//
//@Composable
//fun ClassManagerScreen(
//    onNavigateUp: () -> Unit,
//    onClassClick: (String) -> Unit,
//    onAddClassClick: () -> Unit,
//    viewModel: ClassManagerViewModel = viewModel()
//) {
//    val uiState by viewModel.uiState.collectAsState()
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Quản lý lớp học") },
//                navigationIcon = {
//                    IconButton(onClick = onNavigateUp) {
//                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
//                    }
//                }
//            )
//        },
//        floatingActionButton = {
//            FloatingActionButton(onClick = onAddClassClick) {
//                Icon(Icons.Default.Add, contentDescription = "Thêm lớp học")
//            }
//        }
//    ) { paddingValues ->
//        LazyColumn(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(paddingValues)
//                .padding(16.dp),
//            verticalArrangement = Arrangement.spacedBy(12.dp)
//        ) {
//            items(uiState.classes) { classInfo ->
//                ClassItem(classInfo = classInfo, onClick = { onClassClick(classInfo.id) })
//            }
//        }
//    }
//}
//
//@OptIn(ExperimentalMaterialApi::class)
//@Composable
//fun ClassItem(
//    classInfo: ClassInfo,
//    onClick: () -> Unit
//) {
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        elevation = 4.dp,
//        shape = RoundedCornerShape(8.dp),
//        onClick = onClick
//    ) {
//        Row(
//            modifier = Modifier.padding(12.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Image(
//                painter = painterResource(id = R.drawable.ic_class), // Replace with your icon
//                contentDescription = "Class Icon",
//                modifier = Modifier
//                    .size(50.dp)
//                    .clip(CircleShape)
//            )
//            Spacer(modifier = Modifier.width(16.dp))
//            Column(
//                modifier = Modifier.weight(1f)
//            ) {
//                Text(
//                    text = classInfo.name,
//                    style = MaterialTheme.typography.h6,
//                    fontWeight = FontWeight.Bold,
//                    maxLines = 1,
//                    overflow = TextOverflow.Ellipsis
//                )
//                Spacer(modifier = Modifier.height(4.dp))
//                Text(
//                    text = "Sĩ số: ${classInfo.studentCount}",
//                    style = MaterialTheme.typography.body2,
//                    color = Color.Gray
//                )
//            }
//        }
//    }
//}
//
//@Preview(showBackground = true)
//@Composable
//fun ClassManagerScreenPreview() {
//    DATNTheme {
//        ClassManagerScreen(
//            onNavigateUp = {},
//            onClassClick = {},
//            onAddClassClick = {}
//        )
//    }
//}