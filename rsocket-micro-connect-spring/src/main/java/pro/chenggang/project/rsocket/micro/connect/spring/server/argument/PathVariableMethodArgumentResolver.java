package pro.chenggang.project.rsocket.micro.connect.spring.server.argument;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.messaging.handler.annotation.ValueConstants;
import org.springframework.messaging.handler.annotation.reactive.DestinationVariableMethodArgumentResolver;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * <b>THIS CLASS IS INHERITED FROM {@link org.springframework.messaging.handler.annotation.reactive.DestinationVariableMethodArgumentResolver}</b>
 * Resolve for {@link PathVariable @PathVariable} method parameters.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
public class PathVariableMethodArgumentResolver extends DestinationVariableMethodArgumentResolver {

    public PathVariableMethodArgumentResolver(ConversionService conversionService) {
        super(conversionService);
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(PathVariable.class);
    }

    @Override
    protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
        PathVariable annot = parameter.getParameterAnnotation(PathVariable.class);
        Assert.state(annot != null, "No PathVariable annotation");
        return new PathVariableNamedValueInfo(annot);
    }

    private static final class PathVariableNamedValueInfo extends NamedValueInfo {

        private PathVariableNamedValueInfo(PathVariable annotation) {
            super(annotation.value(), true, ValueConstants.DEFAULT_NONE);
        }
    }
}
