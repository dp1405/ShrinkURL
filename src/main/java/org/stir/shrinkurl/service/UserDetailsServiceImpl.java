// package org.stir.shrinkurl.service;

// import org.springframework.security.core.userdetails.UserDetails;
// import org.springframework.security.core.userdetails.UserDetailsService;
// import org.springframework.security.core.userdetails.UsernameNotFoundException;
// import org.springframework.stereotype.Component;
// import org.stir.shrinkurl.entity.User;

// @Component
// public class UserDetailsServiceImpl implements UserDetailsService {
//     private final UserService userService;

//     public UserDetailsServiceImpl(UserService userService){
//         this.userService=userService;
//     }

//     @Override
//     public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
//         User user = userService.getUserByUsername(username).orElseThrow(() -> new UsernameNotFoundException("Cannot find user with username " + username));

//         return org.springframework.security.core.userdetails.User.builder()
//             .username(user.getUsername())
//             .password(user.getPassword())
//             .roles().build();
//     }
// }
