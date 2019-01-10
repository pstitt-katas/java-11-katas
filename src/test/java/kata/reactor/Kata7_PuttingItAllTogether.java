package kata.reactor;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

interface Renderer {
    Mono<String> createRenderingMono();
}

interface Stub {
    Mono<String> getLanguage();

    Mono<List<String>> getComponents();

    Mono<String> getTemplateMarkup(String language);

    Mono<String> getComponentMarkup(String componentName, String language);

    Mono<String> localise(String content, String language);
}

@Slf4j
class Kata7_PuttingItAllTogether {

    @Test
    void zeroErrors_results_in_expected_content() {
        Renderer renderer = new RendererImpl(createZeroErrorStub());

        StepVerifier.create(renderer.createRenderingMono())
                .expectNext("en{[template: test]}en{[component: Component1]}en{[component: Component2]}en{[component: Component3]}")
                .expectComplete()
                .verify();
    }

    @Test
    void componentError_results_in_responseWithoutTheOffendingComponent() {
        Renderer renderer = new RendererImpl(createComponent2ErrorStub());

        StepVerifier.create(renderer.createRenderingMono())
                .expectNext("en{[template: test]}en{[component: Component1]}en{[component: Component3]}")
                .expectComplete()
                .verify();
    }

    private static final Mono<String> LANGUAGE_MONO = Mono.just("en");
    private static final Mono<List<String>> COMPONENTS_MONO = Mono.just(Arrays.asList("Component1", "Component2", "Component3"));

    private static Stub createZeroErrorStub() {
        Stub stub = Mockito.mock(Stub.class);

        Mockito.when(stub.getLanguage()).thenReturn(LANGUAGE_MONO);
        Mockito.when(stub.getComponents()).thenReturn(COMPONENTS_MONO);
        Mockito.when(stub.getTemplateMarkup(anyString())).thenReturn(markupFor("template", "test"));
        Mockito.when(stub.getComponentMarkup(eq("Component1"), anyString())).thenReturn(markupFor("component", "Component1"));
        Mockito.when(stub.getComponentMarkup(eq("Component2"), anyString())).thenReturn(markupFor("component", "Component2"));
        Mockito.when(stub.getComponentMarkup(eq("Component3"), anyString())).thenReturn(markupFor("component", "Component3"));
        Mockito.when(stub.localise(anyString(), anyString())).thenAnswer(invocation -> {
            String content = invocation.getArgument(0);
            String language = invocation.getArgument(1);
            return Mono.just(format("%s{%s}", language, content));
        });

        return stub;
    }

    private static Stub createComponent2ErrorStub() {
        Stub stub = Mockito.mock(Stub.class);

        Mockito.when(stub.getLanguage()).thenReturn(LANGUAGE_MONO);
        Mockito.when(stub.getComponents()).thenReturn(COMPONENTS_MONO);
        Mockito.when(stub.getTemplateMarkup(anyString())).thenReturn(markupFor("template", "test"));
        Mockito.when(stub.getComponentMarkup(eq("Component1"), anyString())).thenAnswer(componentMarkupAnswer);
        Mockito.when(stub.getComponentMarkup(eq("Component2"), anyString())).thenReturn(Mono.error(new RuntimeException("deliberate error")));
        Mockito.when(stub.getComponentMarkup(eq("Component3"), anyString())).thenAnswer(componentMarkupAnswer);
        Mockito.when(stub.localise(anyString(), anyString())).thenAnswer(localiseAnswer);

        return stub;
    }

    private static Answer<Mono<String>> componentMarkupAnswer = invocation -> {
        String component = invocation.getArgument(0);
        return markupFor("component", component);
    };

    private static Answer<Mono<String>> localiseAnswer = invocation -> {
        String content = invocation.getArgument(0);
        String language = invocation.getArgument(1);
        if (content.isEmpty()) {
            return Mono.just("");
        }
        return Mono.just(format("%s{%s}", language, content));
    };

    private static Mono<String> markupFor(String type, String name) {
        return Mono.just(format("[%s: %s]", type, name));
    }
}

class RendererImpl implements Renderer {
    private final Stub stub;

    RendererImpl(Stub stub) {
        this.stub = stub;
    }

    @Override
    public Mono<String> createRenderingMono() {
        return stub.getLanguage()
                .zipWith(stub.getComponents())
                .flatMap(languageAndComponentListTuple -> {
                    String language = languageAndComponentListTuple.getT1();
                    List<String> components = languageAndComponentListTuple.getT2();

                    return stub.getTemplateMarkup(language)
                            .flatMap(content -> stub.localise(content, language))
                            .zipWith(
                                    Flux.fromIterable(components)
                                            .flatMap(component ->
                                                    stub.getComponentMarkup(component, language)
                                                            .onErrorReturn("")
                                            )
                                            .flatMap(content ->
                                                    stub.localise(content, language)
                                                            .onErrorReturn("")
                                            )
                                            .collectSortedList()
                            )
                            .map(contentTuple -> {
                                String templateContent = contentTuple.getT1();
                                List<String> componentList = contentTuple.getT2();
                                return templateContent + String.join("", componentList);
                            });
                });
    }
}
