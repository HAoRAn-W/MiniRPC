package one.whr.scanner;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.annotation.Annotation;

/**
 * custom package scanner
 */
public class CustomeScanner extends ClassPathBeanDefinitionScanner {
    public CustomeScanner(BeanDefinitionRegistry registry, Class<? extends Annotation> annotationType) {
        super(registry);
        super.addIncludeFilter(new AnnotationTypeFilter(annotationType));
    }

    @Override
    public int scan(String... basePackages) {
        return super.scan(basePackages);
    }
}
