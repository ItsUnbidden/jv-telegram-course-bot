package com.unbidden.telegramcoursesbot.security;

import com.unbidden.telegramcoursesbot.model.Bot;
import com.unbidden.telegramcoursesbot.model.UserEntity;
import com.unbidden.telegramcoursesbot.model.AuthorityType;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation applies security to a post-controller method. That includes
 * testing whether {@link UserEntity} has the required authorities in the {@link Bot}
 * to access the method.

 * <p>The method in question must have several specific parameters 
 * in order for the aspect to fetch the necessary data for access checking:
 * <ul>
 *  <li>{@code Bot bot} that is currently making this request</li>
 *  <li>{@code UserEntity user} that is trying to access the method</li>
 * </ul>
 * @param authorities — required authorities for method access
 * @param botParamName — name of the bot parameter. If not specified,
 * it will be set to {@code "bot"}.
 * @param userParamName — name of the user parameter. If not specified,
 * it will be set to {@code "user"}.
 * @author Unbidden
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Security {
    AuthorityType[] authorities();

    String botParamName() default "bot";

    String userParamName() default "user";
}
