package com.example.user.adapter.out.persistence.mapper;

import com.example.user.adapter.out.persistence.entity.UserEntity;
import com.example.user.domain.model.User;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-06T00:19:23+0900",
    comments = "version: 1.5.5.Final, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.5.jar, environment: Java 21.0.2 (Oracle Corporation)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserEntity toEntity(User user) {
        if ( user == null ) {
            return null;
        }

        UserEntity.UserEntityBuilder userEntity = UserEntity.builder();

        userEntity.email( emailToString( user.getEmail() ) );
        userEntity.id( user.getId() );
        userEntity.name( user.getName() );
        userEntity.password( user.getPassword() );
        userEntity.createdAt( user.getCreatedAt() );
        userEntity.updatedAt( user.getUpdatedAt() );

        return userEntity.build();
    }
}
