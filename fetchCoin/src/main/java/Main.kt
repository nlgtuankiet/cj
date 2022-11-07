import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.closeQuietly
import java.io.RandomAccessFile
import java.util.Locale

data class Model(
    val name: String,
)

var isActive = true

fun main(args: Array<String>) {
    println("args: ${args.toList()}")
    return readFile()
    val url = "https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&per_page=1000"
        .toHttpUrl()
    val client = OkHttpClient()
    val response = client.newCall(Request.Builder().url(url).build()).execute()

    val gson = Gson()
    val type = TypeToken.getParameterized(List::class.java, Model::class.java).type
    val responseModel = gson.fromJson<List<Model>>(response.body?.string() ?: "", type)
    val printModels: List<String> = responseModel.map { it.name }
        .filter { !it.contains(".") }
        .distinct()

    printModels.forEach {
        println("$it")
    }

    println("----")

    printModels.take(25).forEach {
        println(it.lowercase(Locale.getDefault()))
    }
}

fun appendFile() {
}

fun readFile() {
    var file: RandomAccessFile? = null
    Runtime.getRuntime().addShutdownHook(
        Thread {
            println("detech shutdown")
            file?.closeQuietly()
            println("close file done")
            isActive = false
        }
    )
    file = RandomAccessFile("/Users/yata/Development/CryptoJet/text.txt", "rw")
    file.use {
        file.channel.truncate()
    }

    println("end")
}
