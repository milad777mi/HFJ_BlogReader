// در MainActivity.kt (به‌صورت موقت)
import com.hfj.blogreader.data.repository.BlogRepository
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // تست دریافت داده از وبلاگ
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val repo = BlogRepository()
                val posts = repo.fetchAllPosts()
                if (posts.isEmpty()) {
                    Toast.makeText(this@MainActivity, "⚠️ هیچ پستی یافت نشد", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@MainActivity, "✅ ${posts.size} پست دریافت شد", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "❌ خطا: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        setContent {
            Text("✅ در حال تست...")
        }
    }
}
