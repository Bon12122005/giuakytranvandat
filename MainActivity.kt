package com.example.product

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.toObject
import java.util.UUID


data class Product(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val type: String = "",
    val price: String = "",
    val imageUrl: String = ""
)


val PrimaryGreen = Color(0xFF4CAF50)
val DarkGreen = Color(0xFF388E3C)
val LightOrange = Color(0xFFFFCC80)
val LightRed = Color(0xFFFFEBEE)
val BackgroundGray = Color(0xFFF5F5F5)
val ItemBackground = Color(0xFFFFFFFF)

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            FirebaseApp.initializeApp(this)
            auth = FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e("Firebase", "Firebase init error:", e)
        }

        setContent {
            MaterialTheme(
                colors = lightColors(
                    primary = PrimaryGreen,
                    onPrimary = Color.White,
                    background = BackgroundGray,
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Gọi AuthNavigator
                    AuthNavigator(auth = auth)
                }
            }
        }
    }
}


@Composable
fun ProductManagementScreen(auth: FirebaseAuth, currentRole: UserRole) {
    val context = LocalContext.current
    val db = remember { FirebaseFirestore.getInstance() }
    val products = remember { mutableStateListOf<Product>() }


    val canModify = currentRole == UserRole.ADMIN || currentRole == UserRole.MANAGER
    val isAdmin = currentRole == UserRole.ADMIN


    var productName by remember { mutableStateOf("") }
    var productType by remember { mutableStateOf("") }
    var productPrice by remember { mutableStateOf("") }
    var productImageUrl by remember { mutableStateOf("") }

    var isEditing by remember { mutableStateOf(false) }
    var currentProductEdit by remember { mutableStateOf<Product?>(null) }

    val buttonText = if (isEditing) "CẬP NHẬT SẢN PHẨM" else "THÊM SẢN PHẨM"

    fun logout() {
        auth.signOut()
        Toast.makeText(context, "Đã đăng xuất!", Toast.LENGTH_SHORT).show()
    }

    DisposableEffect(Unit) {
        val listener: ListenerRegistration = db.collection("products")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    products.clear()
                    for (doc in snapshot.documents) {
                        val product = doc.toObject<Product>()?.copy(id = doc.id)
                        if (product != null) products.add(product)
                    }
                }
            }
        onDispose { listener.remove() }
    }

    fun reset() {
        productName = ""
        productType = ""
        productPrice = ""
        productImageUrl = ""
        isEditing = false
        currentProductEdit = null
    }

    fun save() {
        if (!canModify) {
            Toast.makeText(context, "Bạn không có quyền thêm/sửa sản phẩm!", Toast.LENGTH_SHORT).show()
            return
        }
        if (productName.isBlank() || productType.isBlank() || productPrice.isBlank() || productImageUrl.isBlank()) {
            Toast.makeText(context, "Vui lòng điền đủ thông tin!", Toast.LENGTH_SHORT).show()
            return
        }


        val p = Product(name = productName, type = productType, price = productPrice, imageUrl = productImageUrl)

        if (isEditing && currentProductEdit != null) {
            db.collection("products").document(currentProductEdit!!.id)
                .set(p)
                .addOnSuccessListener {
                    Toast.makeText(context, "Đã cập nhật!", Toast.LENGTH_SHORT).show()
                    reset()
                }
        } else {
            db.collection("products").add(p)
                .addOnSuccessListener {
                    Toast.makeText(context, "Đã thêm!", Toast.LENGTH_SHORT).show()
                    reset()
                }
        }
    }

    fun delete(p: Product) {
        if (!isAdmin) {
            Toast.makeText(context, "Bạn không có quyền xóa sản phẩm!", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("products").document(p.id).delete()
        if (currentProductEdit?.id == p.id) reset()
    }

    fun edit(p: Product) {
        if (!canModify) return

        isEditing = true
        currentProductEdit = p
        productName = p.name
        productType = p.type
        productPrice = p.price
        productImageUrl = p.imageUrl
    }

    // Hiển thị vai trò trên TopBar
    val roleDisplay = when (currentRole) {
        UserRole.ADMIN -> "Admin"
        UserRole.MANAGER -> "Manager"
        UserRole.VIEWER -> "Viewer"
        else -> ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý Sản phẩm ($roleDisplay)", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color.White) },
                backgroundColor = PrimaryGreen,
                elevation = 8.dp,
                actions = {
                    IconButton(onClick = { logout() }) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = "Đăng xuất", tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (canModify) {
                InputForm(
                    productName, { productName = it },
                    productType, { productType = it },
                    productPrice, { productPrice = it },
                    productImageUrl, { productImageUrl = it },
                    { save() },
                    buttonText
                )
                Spacer(modifier = Modifier.height(24.dp))
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }


            Text("Danh sách sản phẩm:", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DarkGreen)

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(products) { p ->
                    ProductListItem(p,
                        edit = { edit(p) },
                        delete = { delete(p) },
                        canEdit = canModify,
                        canDelete = isAdmin
                    )
                }
            }
        }
    }
}


@Composable
fun CustomTextField(value: String, onChange: (String) -> Unit, hint: String, price: Boolean = false) {
    // ... (Giữ nguyên)
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(hint, color = Color.Gray) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (price) KeyboardType.Number else KeyboardType.Text),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = PrimaryGreen,
            cursorColor = PrimaryGreen
        )
    )
}

@Composable
fun InputForm(
    name: String, onName: (String) -> Unit,
    type: String, onType: (String) -> Unit,
    price: String, onPrice: (String) -> Unit,
    imageUrl: String, onImageUrl: (String) -> Unit,
    onSave: () -> Unit,
    buttonText: String
) {

    Column {
        CustomTextField(name, onName, "Tên sản phẩm")
        Spacer(Modifier.height(8.dp))
        CustomTextField(type, onType, "Loại sản phẩm")
        Spacer(Modifier.height(8.dp))
        CustomTextField(price, onPrice, "Giá", price = true)
        Spacer(Modifier.height(8.dp))
        CustomTextField(imageUrl, onImageUrl, "Ảnh sản phẩm (URL)")
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = PrimaryGreen),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(buttonText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}


@Composable
fun ProductListItem(p: Product, edit: () -> Unit, delete: () -> Unit, canEdit: Boolean, canDelete: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ItemBackground)
            .padding(12.dp)
            .shadow(2.dp, RoundedCornerShape(12.dp)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Image(
            painter = rememberAsyncImagePainter(p.imageUrl),
            contentDescription = p.name,
            modifier = Modifier.size(70.dp).clip(RoundedCornerShape(10.dp)).background(BackgroundGray),
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text("Tên: ${p.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Giá: ${p.price} VNĐ", color = DarkGreen, fontWeight = FontWeight.SemiBold)
            Text("Loại: ${p.type}", color = Color.Gray, fontSize = 12.sp)
        }


        Row {
            if (canEdit) {

                IconButton(onClick = edit, modifier = Modifier
                    .background(LightOrange, RoundedCornerShape(8.dp))
                    .size(36.dp)
                ) {
                    Icon(Icons.Default.Create, "Sửa", tint = Color(0xFFE65100))
                }
            }
            Spacer(Modifier.width(8.dp))
            if (canDelete) {

                IconButton(onClick = delete, modifier = Modifier
                    .background(LightRed, RoundedCornerShape(8.dp))
                    .size(36.dp)
                ) {
                    Icon(Icons.Default.Delete, "Xóa", tint = Color(0xFFD32F2F))
                }
            }
        }
    }
}