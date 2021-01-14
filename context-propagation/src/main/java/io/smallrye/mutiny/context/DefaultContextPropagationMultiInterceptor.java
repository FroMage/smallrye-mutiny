package io.smallrye.mutiny.context;

import io.smallrye.context.SmallRyeContextManagerProvider;
import io.smallrye.context.SmallRyeThreadContext;

/**
 * Provides context propagation to Multi types.
 */
public class DefaultContextPropagationMultiInterceptor extends ContextPropagationMultiInterceptor {

    static final SmallRyeThreadContext THREAD_CONTEXT = SmallRyeContextManagerProvider.instance().getContextManager()
            .newThreadContextBuilder().build();

    @Override
    protected SmallRyeThreadContext getThreadContext() {
        return THREAD_CONTEXT;
    }
}
