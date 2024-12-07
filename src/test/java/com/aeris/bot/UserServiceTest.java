package com.aeris.bot;
import com.aeris.bot.model.User;
import com.aeris.bot.repository.UserRepository;
import com.aeris.bot.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;

class UserServiceTest {

    @Test
    void testRegisterUser() {
        UserRepository mockRepository = Mockito.mock(UserRepository.class);
        UserService userService = new UserService(mockRepository);

        Mockito.when(mockRepository.save(any(User.class))).thenReturn(new User("Test User", "12345"));

        User user = userService.registerUser("Test User", "12345");
        assertNotNull(user);
        assertNotNull(user.getTelegramId());
    }
}