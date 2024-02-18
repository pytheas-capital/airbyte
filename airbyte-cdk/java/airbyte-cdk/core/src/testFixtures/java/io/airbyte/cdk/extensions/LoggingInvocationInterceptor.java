package io.airbyte.cdk.extensions;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.jupiter.api.extension.DynamicTestInvocationContext;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This allows us to have junit loglines in our test logs.
 * This is instanciated via Java's ServiceLoader (https://docs.oracle.com/javase%2F9%2Fdocs%2Fapi%2F%2F/java/util/ServiceLoader.html)
 * The declaration can be found in resources/META-INF/services/org.junit.jupiter.api.extension.Extension
 */
public class LoggingInvocationInterceptor implements InvocationInterceptor {
  private static final class LoggingInvocationInterceptorHandler implements InvocationHandler {
    private static Logger LOGGER = LoggerFactory.getLogger(LoggingInvocationInterceptor.class);
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if (LoggingInvocationInterceptor.class.getDeclaredMethod(method.getName(), Invocation.class, ReflectiveInvocationContext.class, ExtensionContext.class) == null) {
        LOGGER.error("Junit LoggingInvocationInterceptor executing unkown interception point " + method.getName());
        return method.invoke(proxy, args);
      }
      Invocation<?> invocation= (Invocation<?>)args[0];
      ReflectiveInvocationContext<Method> invocationContext = (ReflectiveInvocationContext<Method>)args[1];
      ExtensionContext extensionContext = (ExtensionContext) args[2];
      String methodName = method.getName();
      String logLineSuffix;
      if (methodName.equals("interceptDynamicTest")) {
        logLineSuffix = "execution of DynamicTest " + extensionContext.getDisplayName();
      } else if (methodName.equals("interceptTestClassConstructor")) {
        logLineSuffix = "instance creation for " + invocationContext.getTargetClass();
      } else if (methodName.startsWith("intercept") && methodName.endsWith("Method")) {
        String interceptedEvent =  methodName.substring("intercept".length(), methodName.length() - "Method".length());
        logLineSuffix = "execution of @" + interceptedEvent + " method " + invocationContext.getExecutable().getDeclaringClass().getSimpleName()
            + "." + invocationContext.getExecutable().getName();
      } else {
        logLineSuffix = "execution of unknown intercepted call " + methodName;
      }
      LOGGER.info("Junit starting " + logLineSuffix);
      try {
        long start = System.nanoTime();
        Object retVal = invocation.proceed();
        long elapsedMs = (System.nanoTime() - start)/1_000_000;
        LOGGER.info("Junit completed " + logLineSuffix + " in " + elapsedMs + "ms");
        return retVal;
      } catch (Throwable t) {
        String stackTrace = Arrays.stream(ExceptionUtils.getStackFrames(t)).takeWhile(s->!s.startsWith("\tat org.junit")).collect(
            Collectors.joining("\n  "));
        LOGGER.warn("Junit exception throw during " + logLineSuffix + ":\n" + stackTrace);
        throw t;
      }
    }
  }

  private final InvocationInterceptor proxy = (InvocationInterceptor) Proxy.newProxyInstance(
      getClass().getClassLoader(),
      new Class[] {InvocationInterceptor.class },
      new LoggingInvocationInterceptorHandler());

  @Override
  public void interceptAfterAllMethod(Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext)
      throws Throwable {
    proxy.interceptAfterAllMethod(invocation, invocationContext, extensionContext);
  }

  @Override
  public void interceptAfterEachMethod(Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext)
      throws Throwable {
    proxy.interceptAfterEachMethod(invocation, invocationContext, extensionContext);
  }

  @Override
  public void interceptBeforeAllMethod(Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext)
      throws Throwable {
    proxy.interceptBeforeAllMethod(invocation, invocationContext, extensionContext);
  }

  @Override
  public void interceptBeforeEachMethod(Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext)
      throws Throwable {
    proxy.interceptBeforeEachMethod(invocation, invocationContext, extensionContext);
  }

  @Override
  public void interceptDynamicTest(Invocation<Void> invocation,
      DynamicTestInvocationContext invocationContext, ExtensionContext extensionContext)
      throws Throwable {
    proxy.interceptDynamicTest(invocation, invocationContext, extensionContext);
  }

  @Override
  public void interceptTestMethod(Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext)
      throws Throwable {
    proxy.interceptTestMethod(invocation, invocationContext, extensionContext);
  }

  @Override
  public void interceptTestTemplateMethod(Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext)
      throws Throwable {
    proxy.interceptTestTemplateMethod(invocation, invocationContext, extensionContext);
  }

  @Override
  public <T> T interceptTestFactoryMethod(Invocation<T> invocation,
      ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext)
      throws Throwable {
    return proxy.interceptTestFactoryMethod(invocation, invocationContext, extensionContext);
  }

  @Override
  public <T> T interceptTestClassConstructor(Invocation<T> invocation,
      ReflectiveInvocationContext<Constructor<T>> invocationContext,
      ExtensionContext extensionContext) throws Throwable {
    return proxy.interceptTestClassConstructor(invocation, invocationContext, extensionContext);
  }
}
