package com.example.user.adapter.out.persistence;

import com.example.user.domain.model.User;
import com.example.user.domain.port.UserRepository;
import com.example.user.adapter.out.persistence.entity.UserEntity;
import com.example.user.adapter.out.persistence.mapper.UserMapper;
import io.smallrye.mutiny.converters.uni.UniReactorConverters;
import org.hibernate.reactive.mutiny.Mutiny;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class UserR2dbcRepository implements UserRepository {

    private final Mutiny.SessionFactory sessionFactory;
    private final UserMapper userMapper;

    public UserR2dbcRepository(Mutiny.SessionFactory sessionFactory, UserMapper userMapper) {
        this.sessionFactory = sessionFactory;
        this.userMapper = userMapper;
    }

    @Override
    public Mono<User> save(User user) {
        UserEntity entity = userMapper.toEntity(user);
        return sessionFactory.withTransaction((session, tx) -> session.persist(entity))
                .replaceWith(entity)
                .map(userMapper::toDomain)
                .convert().with(UniReactorConverters.toMono());
    }

    @Override
    public Mono<User> findById(String id) {
        return sessionFactory.withSession(session -> session.find(UserEntity.class, id))
                .map(userMapper::toDomain)
                .convert().with(UniReactorConverters.toMono());
    }

    @Override
    public Mono<User> findByEmail(String email) {
        String query = "from UserEntity where email = :email";
        return sessionFactory.withSession(session -> 
                session.createQuery(query, UserEntity.class)
                       .setParameter("email", email)
                       .getSingleResultOrNull()
        )
        .map(userMapper::toDomain)
        .convert().with(UniReactorConverters.toMono());
    }

    @Override
    public Mono<Boolean> existsByEmail(String email) {
        return findByEmail(email).map(user -> true).defaultIfEmpty(false);
    }
}