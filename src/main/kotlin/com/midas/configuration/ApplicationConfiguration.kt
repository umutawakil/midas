package com.midas.configuration

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.springframework.transaction.PlatformTransactionManager
import java.util.Properties
import javax.sql.DataSource

/**
 * Created by Usman Mutawakil on 6/26/22.
 */
@Configuration
class ApplicationConfiguration(
    @Autowired val dataSource: DataSource,
    @Autowired val applicationProperties: ApplicationProperties
) {
    /*@Bean
    fun entityManagerFactory(): LocalContainerEntityManagerFactoryBean {
        val em                 = LocalContainerEntityManagerFactoryBean()
        em.dataSource          = dataSource
        em.jpaVendorAdapter    = HibernateJpaVendorAdapter()
        em.persistenceUnitName = "mainPersistentUnit"
        em.setPackagesToScan("com.midas.domain")

        val props = Properties()
        props["hibernate.dialect"]                       = applicationProperties.selectedHbmDialect
        props["hibernate.physical_naming_strategy"]      = "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"
        props["hibernate.implicit_naming_strategy"]      = "org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy"
        props["hibernate.current_session_context_class"] = "org.springframework.orm.hibernate5.SpringSessionContext"
        props["hibernate.show_sql"]                      = false
        em.setJpaProperties(props)

        return em
    }

    @Bean
    fun transactionManager(): PlatformTransactionManager? {
        val transactionManager                  = JpaTransactionManager()
        transactionManager.entityManagerFactory = entityManagerFactory().nativeEntityManagerFactory
        return transactionManager
    }*/
}
