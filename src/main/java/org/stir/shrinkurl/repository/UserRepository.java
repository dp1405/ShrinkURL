package org.stir.shrinkurl.repository;

import org.springframework.stereotype.Repository;
import org.stir.shrinkurl.entity.User;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface UserRepository extends JpaRepository<User, Long>{
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    List<User> findByFnameAndLname(String fname,String lname);
    void deleteByEmail(String email);
    void deleteByUsername(String username);
}
