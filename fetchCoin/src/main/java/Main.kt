import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

data class Model(
    val name: String,
)

fun main(args: Array<String>) {
    val url = "https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&per_page=1000"
        .toHttpUrl()
    val client = OkHttpClient()
    val response = client.newCall(Request.Builder().url(url).build()).execute()

    val gson = Gson()
    val type = TypeToken.getParameterized(List::class.java, Model::class.java).type
    val responseModel = gson.fromJson<List<Model>>(response.body?.string() ?: "", type)
    val printModels = responseModel.map { it.name }
        .filter { !it.contains(".") }
        .distinct()

    printModels.forEach {
        println("- $it")
    }

    println("----")

    printModels.take(20).forEach {
        println("- ${it.toLowerCase()} widget")
    }
}
