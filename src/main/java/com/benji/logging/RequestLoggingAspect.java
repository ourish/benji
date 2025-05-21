package com.benji.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class RequestLoggingAspect {

    @Around("execution(* com.benji.controllers.WalletController..*(..))")
    public Object logRequests(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        log.info("Incoming request: {} | Arguments: {}",
                joinPoint.getSignature().toShortString(),
                Arrays.toString(joinPoint.getArgs()));

        Object result = joinPoint.proceed(); // Execute the actual method

        long duration = System.currentTimeMillis() - startTime;
        log.info("Completed request: {} | Execution time: {} ms",
                joinPoint.getSignature().toShortString(),
                duration);

        return result;
    }

}
