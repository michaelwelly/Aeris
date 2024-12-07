package com.aeris.bot;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.aeris.bot.model.User;
import com.aeris.bot.repository.UserRepository;
import com.aeris.bot.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {

    @Test
    void testRegisterUser() {
        // Мокируем UserRepository
        UserRepository mockRepository = Mockito.mock(UserRepository.class);

        // Создаем сервис, который использует мокированный репозиторий
        UserService userService = new UserService(mockRepository);

        // Мокаем findByTelegramId, чтобы он возвращал null (пользователь не существует)
        when(mockRepository.findByTelegramId("12345")).thenReturn(null);

        // Мокаем метод save, чтобы он возвращал нового пользователя
        when(mockRepository.save(any(User.class))).thenReturn(new User("Test", "User", "12345", "testuser", "en"));

        // Вызов метода, который тестируется
        User user = userService.registerUser("Test", "User", "12345", "testuser", "en");

        // Проверки
        assertNotNull(user); // Убедимся, что пользователь не null
        assertNotNull(user.getTelegramId()); // Проверяем, что у пользователя есть telegramId
        assertEquals("Test", user.getFirstName()); // Проверяем, что имя пользователя правильное
        assertEquals("User", user.getLastName()); // Проверяем, что фамилия правильная
        assertEquals("12345", user.getTelegramId()); // Проверяем, что telegramId правильное
        assertEquals("testuser", user.getUsername()); // Проверяем, что username правильное
        assertEquals("en", user.getLanguageCode()); // Проверяем, что languageCode правильное

        // Проверяем, что метод findByTelegramId был вызван с нужным аргументом
        verify(mockRepository, times(1)).findByTelegramId("12345");

        // Проверяем, что метод save был вызван один раз
        verify(mockRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegisterUserAlreadyExists() {
        UserRepository mockRepository = Mockito.mock(UserRepository.class);
        UserService userService = new UserService(mockRepository);

        // Мокаем findByTelegramId, чтобы он возвращал существующего пользователя
        when(mockRepository.findByTelegramId("12345")).thenReturn(new User("Existing", "User", "12345", "existinguser", "en"));

        // Проверяем, что будет выброшено исключение при попытке зарегистрировать пользователя
        assertThrows(RuntimeException.class, () -> userService.registerUser("Test", "User", "12345", "testuser", "en"));
    }
}
