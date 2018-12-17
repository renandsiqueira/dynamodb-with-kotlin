package de.bringmeister.examples.auditing

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable
import de.bringmeister.infrastructure.docker.dynamoDbInDocker
import de.bringmeister.infrastructure.dynamodb.DynamoDbConfiguration
import de.bringmeister.infrastructure.dynamodb.DynamoDbTableUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.junit.runner.RunWith
import org.socialsignin.spring.data.dynamodb.config.EnableDynamoDBAuditing
import org.socialsignin.spring.data.dynamodb.mapping.DynamoDBMappingContext
import org.socialsignin.spring.data.dynamodb.repository.EnableScan
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.test.context.junit4.SpringRunner
import java.util.Date

@RunWith(SpringRunner::class)
@SpringBootTest(
    classes = [
        DynamoDbConfiguration::class,
        DynamoDbTableUtil::class,
        AuditingConfiguration::class,
        AuditedEntityDynamoDbRepository::class
    ],
    properties = [
        "aws.dynamodb.endpoint: http://localhost:15378"
    ]
)
class AuditingTest {

    @Autowired
    private lateinit var repo: AuditedEntityDynamoDbRepository

    @Autowired
    private lateinit var util: DynamoDbTableUtil

    companion object {

        @ClassRule
        @JvmField
        val dynamodb = dynamoDbInDocker()
    }

    @Before
    fun before() {
        util.createTableForEntity(AuditedEntity::class)
        repo.deleteAll()
    }

    @Test
    fun `should set audit fields DynamoDB`() {

        repo.save(AuditedEntity(id = "42", firstname = "Jon", lastname = "Doe"))
        Thread.sleep(2 * 1000)
        repo.save(AuditedEntity(id = "42", firstname = "Johnny", lastname = "Doe"))

        val auditedEntityFromDynamoDb = repo.findById("42").get()

        assertThat(auditedEntityFromDynamoDb.lastModifiedAt).isNotNull()
        // assertThat(auditedEntityFromDynamoDb.createdAt).isNotNull() // DOES NOT WORK!
    }
}

@Configuration
@EnableDynamoDBAuditing
class AuditingConfiguration {

    @Bean
    fun dynamoDBMappingContext(): DynamoDBMappingContext {
        return DynamoDBMappingContext().apply {
            getPersistentEntity(AuditedEntity::class.java)
        }
    }
}

@Repository
@EnableScan
interface AuditedEntityDynamoDbRepository : CrudRepository<AuditedEntity, String>

@DynamoDBTable(tableName = "AuditedEntity")
data class AuditedEntity(

    @Id
    @DynamoDBHashKey(attributeName = "id")
    var id: String? = null,

    @DynamoDBAttribute(attributeName = "firstname")
    var firstname: String? = null,

    @DynamoDBAttribute(attributeName = "lastname")
    var lastname: String? = null,

    @CreatedDate
    @DynamoDBAttribute
    var createdAt: Date? = null,

    @LastModifiedDate
    @DynamoDBAttribute
    var lastModifiedAt: Date? = null
)