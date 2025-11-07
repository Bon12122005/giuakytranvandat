package com.example.product

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material.CircularProgressIndicator
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await // Cần thêm dependency cho coroutines-play-services

// Enum cho các vai trò người dùng
enum class UserRole {
    LOADING,    // Đang chờ đọc vai trò từ Firestore
    UNAUTHORIZED, // Chưa đăng nhập hoặc bị chặn
    VIEWER,     // Chỉ xem sản phẩm
    MANAGER,    // Có thể thêm sản phẩm
    ADMIN       // Có thể Thêm/Sửa/Xóa sản phẩm
}

// ---------------------------------------------------------------------
// --- HÀM LẤY VAI TRÒ TỪ FIRESTORE ---
// ---------------------------------------------------------------------
// Lấy Vai trò của người dùng từ Firestore (Collection: users, Document ID: uid)
suspend fun fetchUserRole(uid: String): UserRole {
    val db = FirebaseFirestore.getInstance()
    return try {
        val document = db.collection("users").document(uid).get().await()
        val roleString = document.getString("role")?.uppercase()

        when (roleString) {
            "ADMIN" -> UserRole.ADMIN
            "MANAGER" -> UserRole.MANAGER
            "VIEWER" -> UserRole.VIEWER
            else -> UserRole.VIEWER // Vai trò mặc định nếu không có hoặc không hợp lệ
        }
    } catch (e: Exception) {
        // Log.e("Auth", "Error fetching user role", e)
        UserRole.VIEWER // Trả về vai trò cơ bản nhất nếu có lỗi
    }
}



@Composable
fun AuthNavigator(auth: FirebaseAuth) {
    val currentUser = remember { mutableStateOf(auth.currentUser) }

    var userRole by remember { mutableStateOf(UserRole.LOADING) }


    DisposableEffect(auth) {
        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser.value = firebaseAuth.currentUser
            // Nếu có người dùng, reset vai trò để bắt đầu load lại
            userRole = if (firebaseAuth.currentUser != null) UserRole.LOADING else UserRole.UNAUTHORIZED
        }
        auth.addAuthStateListener(authStateListener)
        onDispose {
            auth.removeAuthStateListener(authStateListener)
        }
    }


    if (currentUser.value != null && userRole == UserRole.LOADING) {
        val uid = currentUser.value!!.uid
        LaunchedEffect(uid) {
            userRole = fetchUserRole(uid)
        }
    }


    when (userRole) {
        UserRole.UNAUTHORIZED -> LoginScreen(auth = auth)
        UserRole.LOADING -> FullScreenLoading()
        else -> ProductManagementScreen(auth = auth, currentRole = userRole)
    }
}


@Composable
fun FullScreenLoading() {
    Box(
        modifier = Modifier.fillMaxSize().background(BackgroundGray),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = PrimaryGreen)
            Spacer(Modifier.height(16.dp))
            Text("Đang tải dữ liệu người dùng...", color = DarkGreen)
        }
    }
}


@Composable
fun LoginScreen(auth: FirebaseAuth) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    fun login() {
        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(context, "Vui lòng nhập Email và Mật khẩu!", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                isLoading = false
                if (task.isSuccessful) {
                    Toast.makeText(context, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()

                } else {
                    Toast.makeText(context, "Đăng nhập thất bại: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .background(BackgroundGray),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("ĐĂNG NHẬP HỆ THỐNG", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = DarkGreen)
        Spacer(Modifier.height(32.dp))


        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = PrimaryGreen,
                cursorColor = PrimaryGreen
            )
        )
        Spacer(Modifier.height(16.dp))


        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mật khẩu") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = PrimaryGreen,
                cursorColor = PrimaryGreen
            )
        )
        Spacer(Modifier.height(24.dp))


        Button(
            onClick = { login() },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryGreen),
            enabled = !isLoading,
            shape = RoundedCornerShape(10.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("ĐĂNG NHẬP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}