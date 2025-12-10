# Common Profile Management System - Integration Guide

## Overview
A unified, role-agnostic profile management system in the `common` folder that handles Student, Teacher, and Parent profile editing with a single ViewModel, State, and UI Screen.

## Architecture

### Files Created

#### Common Layer (presentation/common/profile/)
```
presentation/common/profile/
├── EditProfileState.kt          # Unified state for all roles
├── EditProfileEvent.kt          # Unified events for all roles
├── EditProfileViewModel.kt      # Unified ViewModel for all roles
└── EditProfileScreen.kt         # Unified UI Screen for all roles
```

#### Domain Layer (Use Cases - Already Created)
```
domain/usecase/student/
├── GetStudentProfileUseCase.kt
└── UpdateStudentProfileUseCase.kt

domain/usecase/teacher/
├── GetTeacherProfileUseCase.kt
└── UpdateTeacherProfileUseCase.kt

domain/usecase/parent/
├── GetParentProfileUseCase.kt
└── UpdateParentProfileUseCase.kt
```

#### Updated Account Screens
```
presentation/student/account/StudentAccountScreen.kt
presentation/teacher/account/TeacherAccountScreen.kt
presentation/parent/account/ParentAccountScreen.kt
```

## State Management

### EditProfileState
```kotlin
data class EditProfileState(
    // Common fields
    val isLoading: Boolean = false
    val error: String? = null
    val isSuccess: Boolean = false
    
    // Student specific
    val student: Student? = null
    val gradeLevel: String = ""
    val dateOfBirth: LocalDate? = null
    
    // Teacher specific
    val teacher: Teacher? = null
    val specialization: String = ""
    val level: String = ""
    val experienceYears: String = "0"
    
    // Parent specific
    val parent: Parent? = null
)
```

### EditProfileEvent
```kotlin
sealed class EditProfileEvent {
    // Common events
    data class LoadProfile(val userId: String, val role: String) : EditProfileEvent()
    object SaveProfile : EditProfileEvent()
    object ClearMessages : EditProfileEvent()
    
    // Student specific events
    data class UpdateGradeLevel(val gradeLevel: String) : EditProfileEvent()
    data class UpdateDateOfBirth(val dateOfBirth: LocalDate) : EditProfileEvent()
    
    // Teacher specific events
    data class UpdateSpecialization(val specialization: String) : EditProfileEvent()
    data class UpdateLevel(val level: String) : EditProfileEvent()
    data class UpdateExperienceYears(val years: String) : EditProfileEvent()
}
```

## Navigation Integration

### Step 1: Add Route to Navigation Graph

```kotlin
composable(
    route = "edit_profile/{userId}/{role}",
    arguments = listOf(
        navArgument("userId") { type = NavType.StringType },
        navArgument("role") { type = NavType.StringType }
    )
) { backStackEntry ->
    val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
    val role = backStackEntry.arguments?.getString("role") ?: return@composable
    
    EditProfileScreen(
        userId = userId,
        role = role,
        onNavigateBack = { navController.popBackStack() }
    )
}
```

### Step 2: Update Account Screen Navigation Calls

#### For StudentAccountScreen
```kotlin
StudentAccountScreen(
    onNavigateToLogin = { navController.navigate("login") },
    onNavigateToChangePassword = { navController.navigate("student_change_password") },
    onNavigateToEditProfile = { userId, role ->
        navController.navigate("edit_profile/$userId/$role")
    }
)
```

#### For TeacherAccountScreen
```kotlin
TeacherAccountScreen(
    onNavigateToLogin = { navController.navigate("login") },
    onNavigateToChangePassword = { navController.navigate("teacher_change_password") },
    onNavigateToEditProfile = { userId, role ->
        navController.navigate("edit_profile/$userId/$role")
    }
)
```

#### For ParentAccountScreen
```kotlin
ParentAccountScreen(
    onNavigateToLogin = { navController.navigate("login") },
    onNavigateToChangePassword = { navController.navigate("parent_change_password") },
    onNavigateToEditProfile = { userId, role ->
        navController.navigate("edit_profile/$userId/$role")
    }
)
```

## Usage Examples

### Loading a Student Profile
```kotlin
viewModel.onEvent(EditProfileEvent.LoadProfile("student123", "STUDENT"))
```

### Updating Student Information
```kotlin
// Update grade level
viewModel.onEvent(EditProfileEvent.UpdateGradeLevel("10A"))

// Update date of birth
viewModel.onEvent(EditProfileEvent.UpdateDateOfBirth(LocalDate.of(2008, 5, 15)))

// Save changes
viewModel.onEvent(EditProfileEvent.SaveProfile)
```

### Loading a Teacher Profile
```kotlin
viewModel.onEvent(EditProfileEvent.LoadProfile("teacher456", "TEACHER"))
```

### Updating Teacher Information
```kotlin
// Update specialization
viewModel.onEvent(EditProfileEvent.UpdateSpecialization("Toán học"))

// Update level
viewModel.onEvent(EditProfileEvent.UpdateLevel("Thạc sĩ"))

// Update experience years
viewModel.onEvent(EditProfileEvent.UpdateExperienceYears("5"))

// Save changes
viewModel.onEvent(EditProfileEvent.SaveProfile)
```

### Loading a Parent Profile
```kotlin
viewModel.onEvent(EditProfileEvent.LoadProfile("parent789", "PARENT"))
```

### Saving Parent Profile
```kotlin
viewModel.onEvent(EditProfileEvent.SaveProfile)
```

## Screen Behavior

### EditProfileScreen
- **Dynamic Title**: Changes based on role
  - Student: "Chỉnh sửa hồ sơ học sinh"
  - Teacher: "Chỉnh sửa hồ sơ giáo viên"
  - Parent: "Chỉnh sửa hồ sơ phụ huynh"

- **Dynamic Fields**: Renders different fields based on role
  - **Student**: Grade Level, Date of Birth (with interactive date picker)
  - **Teacher**: Specialization, Level, Experience Years
  - **Parent**: Profile information display

- **Common Features**:
  - Back navigation button
  - Loading indicator
  - Error message display
  - Success notification
  - Auto-navigate on success

## ViewModel Logic

### EditProfileViewModel
The ViewModel handles:

1. **Profile Loading**
   - Dispatches to appropriate use case based on role
   - Updates state with loaded data
   - Handles loading/error states

2. **Field Updates**
   - Updates state for each field change
   - Validates input (numeric for experience years)

3. **Profile Saving**
   - Determines which role to save
   - Calls appropriate update use case
   - Handles success/error responses
   - Shows notifications via NotificationManager

4. **Error Handling**
   - Displays user-friendly error messages
   - Logs errors for debugging
   - Clears messages on demand

## Event Flow Diagram

```
UI (EditProfileScreen)
    ↓
Event (EditProfileEvent)
    ↓
ViewModel (EditProfileViewModel)
    ↓
Use Case (GetStudentProfileUseCase, etc.)
    ↓
Service (StudentService, TeacherService, ParentService)
    ↓
Firestore (Data Storage)
    ↓
State Update (EditProfileState)
    ↓
UI Recomposition
```

## Features

✅ **Unified System**
- Single ViewModel for all roles
- Single State for all roles
- Single UI Screen for all roles
- Reduces code duplication

✅ **Role-Specific Behavior**
- Different fields per role
- Different validation rules
- Different save logic
- Adaptive UI

✅ **Proper State Management**
- Sealed classes for type-safe events
- Data classes for immutable state
- StateFlow for reactive updates
- Proper error handling

✅ **User Experience**
- Loading states with progress indicators
- Error messages with user guidance
- Success notifications
- Auto-navigation on success
- Back navigation support
- Scrollable forms for long content

✅ **Code Quality**
- MVVM architecture
- Dependency injection with Hilt
- Reactive programming with Flow
- Material Design 3 styling
- Proper separation of concerns

## Testing

To test the integration:

1. Navigate to Student/Teacher/Parent Account Screen
2. Click "Chỉnh sửa hồ sơ" button
3. Verify correct fields appear based on role
4. Update the profile information
5. Click "Lưu thay đổi" button
6. Verify success notification appears
7. Verify automatic navigation back to account screen

## Troubleshooting

### Issue: "No value passed for parameter" errors
**Solution**: Ensure all navigation callbacks are properly passed to the account screens with correct signature `(String, String) -> Unit`.

### Issue: Profile not loading
**Solution**: 
- Verify the userId is correct
- Check that the role is one of: "STUDENT", "TEACHER", "PARENT"
- Ensure the appropriate service is properly injected

### Issue: Changes not saving
**Solution**: 
- Check that the update use cases are properly implemented
- Verify Firestore permissions allow write operations
- Check network connectivity

### Issue: Wrong fields appearing
**Solution**: Verify the role parameter is correctly passed (case-sensitive: "STUDENT", "TEACHER", "PARENT")

## Project Structure Impact

✅ **No Breaking Changes**
- Existing account screens remain functional
- New common profile system is additive
- No modifications to existing models
- No modifications to existing services

✅ **Clean Architecture**
- Follows MVVM pattern
- Proper separation of concerns
- Reusable components
- Maintainable code

## Dependencies

- Kotlin Flow & StateFlow
- Jetpack Compose
- Hilt Dependency Injection
- Firestore
- Material Design 3
- Java Time API (LocalDate)

## Notes

- All screens follow MVVM architecture
- Events are handled through sealed classes
- State is managed reactively with StateFlow
- Notifications are shown via NotificationManager
- All user inputs are validated before saving
- The system is extensible for future roles
