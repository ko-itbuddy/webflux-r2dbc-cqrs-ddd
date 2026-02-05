package com.example.user.adapter.out.persistence.mapper;

import com.example.common.domain.valueobject.Email;
import com.example.user.domain.model.User;
import com.example.user.adapter.out.persistence.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "email", source = "email", qualifiedByName = "emailToString")
    UserEntity toEntity(User user);

    default User toDomain(UserEntity entity) {
        if (entity == null) return null;
        return User.reconstitute(
            entity.getId(),
            entity.getName(),
            Email.of(entity.getEmail()),
            entity.getPassword(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    @Named("emailToString")
    default String emailToString(Email email) {
        return email != null ? email.getValue() : null;
    }
}
