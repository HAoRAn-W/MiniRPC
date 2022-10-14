package one.whr.annotation;

import one.whr.scanner.CustomScannerRegistra;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记服务所在的base package, 用于spring的扫描
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Import(CustomScannerRegistra.class)
@Documented
public @interface RpcScan {
    String[] basePackage();
}
