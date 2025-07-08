package org.stir.shrinkurl.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stir.shrinkurl.entity.Subscription;
import org.stir.shrinkurl.entity.User;
import org.stir.shrinkurl.repository.SubscriptionRepository;
import org.stir.shrinkurl.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UserService {
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    // private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, SubscriptionRepository subscriptionRepository){
        this.userRepository=userRepository;
        this.subscriptionRepository=subscriptionRepository;
        // this.passwordEncoder=passwordEncoder;
    }

    @Transactional(readOnly=true)
    public List<User> getAllUsers(){
        try{
            return userRepository.findAll();
        }
        catch(Exception e){
            log.error("Failed getting All users!", e);
            return Collections.emptyList();
        }
    }

    @Transactional
    public boolean saveNewUser(User user){
        try{
            // user.setPassword(passwordEncoder.encode(user.getPassword()));
            userRepository.save(user);
            return true;
        }
        catch(Exception e){
            log.error("Failed Saving User!", e);
            return false;
        }
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserByID(Long id){
        try{
            return userRepository.findById(id);
        }
        catch(Exception e){
            log.error("Failed finding user by ID!", e);
            return Optional.empty();
        }
    }

    @Transactional
    public void deleteUserById(Long id){
        try{
            userRepository.deleteById(id);
        }
        catch(Exception e){
            log.error("Failed deleting user by ID!", e);
        }
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserByEmail(String email){
        try{
            return userRepository.findByEmail(email);
        }
        catch(Exception e){
            log.error("Failed finding user by Email!", e);
            return Optional.empty();
        }
    }

    @Transactional
    public void deleteUserByEmail(String email){
        try{
            userRepository.deleteByEmail(email);
        }
        catch(Exception e){
            log.error("Failed deleting user by Email!", e);
        }
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserByUsername(String username){
        try{
            return userRepository.findByUsername(username);
        }
        catch(Exception e){
            log.error("Failed finding user by username!", e);
            return Optional.empty();
        }
    }

    @Transactional
    public void deleteUserByUsername(String username){
        try{
            userRepository.deleteByUsername(username);
        }
        catch(Exception e){
            log.error("Failed deleting user by Email!", e);
        }
    }

    @Transactional
    public boolean addOrUpdateSubscriptionDetails(Long userId, Subscription newSubscription){
        try{
            User user = getUserByID(userId).orElse(null);
            if(user==null){
                return false;
            }

            Subscription existing=user.getSubscription();

            if(existing==null || existing.getActive()==false){
                newSubscription.setStartTime(LocalDateTime.now());
                newSubscription.setActive(true);

                newSubscription.setUser(user);
                user.setSubscription(newSubscription);
                subscriptionRepository.save(newSubscription);
            }
            else{
                existing.setDuration(newSubscription.getDuration()+existing.getDuration());
                subscriptionRepository.save(existing);
            }

            userRepository.save(user);
            return true;
        }
        catch(Exception e){
            log.error("Failed adding or updating subscription details!", e);
            return false;
        }
    }

    @Transactional
    public boolean checkSubscriptionStatus(Long userId){
        try{
            User user=getUserByID(userId).orElse(null);
            if(user==null){
                return false;
            }

            Subscription subscription=user.getSubscription();
            if(subscription==null || subscription.getActive()==false){
                return false;
            }

            LocalDateTime expiry=subscription.getStartTime().plusYears(subscription.getDuration());
            if(LocalDateTime.now().isBefore(expiry)){
                return true;
            }

            subscription.setActive(false);
            return false;
        }
        catch(Exception e){
            log.error("Failed obtaining subscription status!",e);
            return false;
        }
    }
}
