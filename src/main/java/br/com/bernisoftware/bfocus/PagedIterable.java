package br.com.bernisoftware.bfocus;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Todos os itens de uma listagem paginada, buscados página a página SOB DEMANDA (preguiçoso): nenhuma requisição
 * acontece até você começar a percorrer. Cada página é uma chamada nova (com {@code X-Request-Id} próprio);
 * para na última página ou numa página vazia.
 *
 * <pre>{@code
 * for (Customer c : client.customers().listAll()) { ... }
 * client.kb().articles().listAll().stream().filter(a -> "draft".equals(a.getStatus())).count();
 * }</pre>
 *
 * <p>Cada {@link #iterator()} (ou {@link #stream()}) recomeça da página 1. Um erro da API numa página no meio
 * do caminho é lançado durante a iteração ({@link BfocusException}).
 *
 * @param <T> tipo do item
 */
public final class PagedIterable<T> implements Iterable<T> {
    /** Busca a página {@code n} (a partir de 1). */
    interface Fetcher<T> {
        Page<T> fetch(int page);
    }

    private final Fetcher<T> fetcher;

    PagedIterable(Fetcher<T> fetcher) {
        this.fetcher = fetcher;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private Iterator<T> current = Collections.emptyIterator();
            private int nextPage = 1;
            private boolean last;

            @Override
            public boolean hasNext() {
                while (!current.hasNext()) {
                    if (last) {
                        return false;
                    }
                    int requested = nextPage++;
                    Page<T> page = fetcher.fetch(requested);
                    current = page.getItems().iterator();
                    if (page.getItems().isEmpty() || requested >= page.getPages()) {
                        last = true;
                    }
                }
                return true;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return current.next();
            }
        };
    }

    /**
     * Os itens como {@link Stream} sequencial e preguiçoso.
     *
     * @return o stream
     */
    public Stream<T> stream() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED | Spliterator.NONNULL), false);
    }
}
