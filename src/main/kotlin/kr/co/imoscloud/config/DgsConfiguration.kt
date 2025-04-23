package kr.co.imoscloud.config

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsRuntimeWiring
import graphql.scalars.ExtendedScalars
import graphql.schema.idl.RuntimeWiring
import org.springframework.context.annotation.Configuration

@Configuration
@DgsComponent
class DgsConfiguration {
    @DgsRuntimeWiring
    fun addScalars(builder: RuntimeWiring.Builder): RuntimeWiring.Builder {
        // graphql-java-extended-scalars 라이브러리의 DateTime 스칼라 사용
        return builder.scalar(ExtendedScalars.DateTime)
    }
}