package space.hugster.calendar.database.impl

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import org.bson.Document
import space.hugster.calendar.config.CalendarConfig
import space.hugster.calendar.data.CalendarProfile
import space.hugster.calendar.database.StorageProvider
import java.util.UUID

class MongoStorageProvider : StorageProvider
{
    private lateinit var client: MongoClient
    private lateinit var collection: MongoCollection<Document>
    private val upsertOptions = ReplaceOptions().upsert(true)

    override fun init()
    {
        client = MongoClients.create(
            CalendarConfig.MONGO_URI)
        val database = client.getDatabase(
            CalendarConfig.MONGO_DATABASE)
        collection = database.getCollection(
            CalendarConfig.MONGO_COLLECTION)
    }

    override fun loadProfile(uuid: UUID): CalendarProfile
    {
        val doc = collection.find(Filters.eq("_id", uuid.toString())).first()
            ?: return CalendarProfile(uuid, 0)

        val mask = doc.getInteger("claimed_mask", 0)
        return CalendarProfile(uuid, mask)
    }

    override fun saveProfile(profile: CalendarProfile)
    {
        val doc = Document("_id", profile.uuid.toString())
            .append("claimed_mask", profile.claimedMask)

        collection.replaceOne(
            Filters.eq("_id", profile.uuid.toString()),
            doc,
            upsertOptions
        )
    }

    override fun resetAll()
    {
        collection.deleteMany(Document())
    }

    override fun shutdown()
    {
        if (::client.isInitialized) {
            client.close()
        }
    }
}
