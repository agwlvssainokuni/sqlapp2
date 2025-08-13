/*
 * Copyright 2025 agwlvssainokuni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cherry.sqlapp2.aspect;

import jakarta.annotation.Nonnull;
import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.interceptor.CustomizableTraceInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.Optional;

@Component
@Aspect
public class TraceAspect {

    private final CustomizableTraceInterceptor traceInterceptor;

    public TraceAspect(
            @Value("${app.trace.useDynamicLogger}") boolean useDynamicLogger,
            @Value("${app.trace.hideProxyClassNames}") boolean hideProxyClassNames,
            @Value("${app.trace.logExceptionStackTrace}") boolean logExceptionStackTrace,
            @Value("${app.trace.enterMessage}") String enterMessage,
            @Value("${app.trace.exitMessage}") String exitMessage,
            @Value("${app.trace.exceptionMessage}") String exceptionMessage
    ) {
        traceInterceptor = new CustomizableTraceInterceptor();
        traceInterceptor.setUseDynamicLogger(useDynamicLogger);
        traceInterceptor.setHideProxyClassNames(hideProxyClassNames);
        traceInterceptor.setLogExceptionStackTrace(logExceptionStackTrace);
        traceInterceptor.setEnterMessage(enterMessage);
        traceInterceptor.setExitMessage(exitMessage);
        traceInterceptor.setExceptionMessage(exceptionMessage);
    }

    @Around("""
            execution(* cherry.sqlapp2.controller.*.*(..))
            || execution(* cherry.sqlapp2.service.*.*(..))
            || execution(* cherry.sqlapp2.repository.*.*(..))
            """)
    public Object trace(ProceedingJoinPoint joinPoint) throws Throwable {
        return traceInterceptor.invoke(
                new ProceedingJoinPointMethodInvocation(joinPoint)
        );
    }

    static class ProceedingJoinPointMethodInvocation implements MethodInvocation {
        private final ProceedingJoinPoint joinPoint;

        ProceedingJoinPointMethodInvocation(ProceedingJoinPoint joinPoint) {
            this.joinPoint = joinPoint;
        }

        @Override
        public Object proceed() throws Throwable {
            return joinPoint.proceed();
        }

        @Nonnull
        @Override
        public Object[] getArguments() {
            return joinPoint.getArgs();
        }

        @Override
        public Object getThis() {
            return joinPoint.getThis();
        }

        @Nonnull
        @Override
        public Method getMethod() {
            return Optional.of(joinPoint).map(ProceedingJoinPoint::getSignature)
                    .filter(MethodSignature.class::isInstance).map(MethodSignature.class::cast)
                    .map(MethodSignature::getMethod)
                    .get();
        }

        @Nonnull
        @Override
        public AccessibleObject getStaticPart() {
            return getMethod();
        }
    }

}
