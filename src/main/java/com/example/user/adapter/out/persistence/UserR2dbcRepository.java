package com.example.user.adapter.out.persistence;

import com.example.user.domain.model.User;
import com.example.user.domain.port.UserRepository;
import io.smallrye.mutiny.converters.uni.UniReactorConverters;
import org.hibernate.reactive.mutiny.Mutiny;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class UserR2dbcRepository implements UserRepository {

    private final Mutiny.SessionFactory sessionFactory;

    public UserR2dbcRepository(Mutiny.SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Mono<User> save(User user) {
        return sessionFactory.withTransaction((session, tx) -> session.merge(user))
                .convert().with(UniReactorConverters.toMono());
    }

    @Override
    public Mono<User> findById(String id) {
        return sessionFactory.withSession(session -> session.find(User.class, id))
                .convert().with(UniReactorConverters.toMono());
    }

    @Override
    public Mono<User> findByEmail(String email) {
        String query = "from User where email.value = :email";
        return sessionFactory.withSession(session -> 
                session.createQuery(query, User.class)
                       .setParameter("email", email)
                       .getSingleResultOrNull()
        )
        .convert().with(UniReactorConverters.toMono());
    }

    @Override
    public Mono<Boolean> existsByEmail(String email) {
        return findByEmail(email).map(user -> true).defaultIfEmpty(false);
    }
}
