package br.com.bernisoftware.bfocus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Uma página de uma listagem paginada (imutável). Iterável: {@code for (Customer c : page)}.
 *
 * <p>O número da página é {@link #getPageNumber()} (no JSON da API continua {@code page}).
 *
 * @param <T> tipo do item
 */
public final class Page<T> implements Iterable<T> {
    private final List<T> items;
    private final int pageNumber;
    private final int pageSize;
    private final int total;
    private final int pages;

    /**
     * Cria a página (útil para simular respostas nos testes da sua aplicação).
     *
     * @param items itens desta página
     * @param pageNumber número desta página (a partir de 1)
     * @param pageSize tamanho da página
     * @param total total de itens em todas as páginas
     * @param pages total de páginas
     */
    public Page(List<T> items, int pageNumber, int pageSize, int total, int pages) {
        this.items = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(items, "items")));
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.total = total;
        this.pages = pages;
    }

    /**
     * Itens desta página.
     *
     * @return lista imutável
     */
    public List<T> getItems() {
        return items;
    }

    /**
     * Número desta página (a partir de 1). JSON: {@code page}.
     *
     * @return o número
     */
    public int getPageNumber() {
        return pageNumber;
    }

    /**
     * Tamanho da página.
     *
     * @return o tamanho
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * Total de itens em todas as páginas.
     *
     * @return o total
     */
    public int getTotal() {
        return total;
    }

    /**
     * Total de páginas.
     *
     * @return o total de páginas
     */
    public int getPages() {
        return pages;
    }

    /**
     * Há página depois desta.
     *
     * @return {@code true} se há próxima página
     */
    public boolean hasNextPage() {
        return !items.isEmpty() && pageNumber < pages;
    }

    @Override
    public Iterator<T> iterator() {
        return items.iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Page)) {
            return false;
        }
        Page<?> other = (Page<?>) o;
        return pageNumber == other.pageNumber && pageSize == other.pageSize && total == other.total
                && pages == other.pages && items.equals(other.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(items, pageNumber, pageSize, total, pages);
    }

    @Override
    public String toString() {
        return "Page{page=" + pageNumber + ", pageSize=" + pageSize + ", total=" + total + ", pages=" + pages
                + ", items=" + items + "}";
    }
}
