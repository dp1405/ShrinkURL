package org.stir.shrinkurl.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.stir.shrinkurl.service.UserService;

public class UserController {
    private final UserService userService;


    public UserController(UserService userService){
        this.userService=userService;
    }


}
