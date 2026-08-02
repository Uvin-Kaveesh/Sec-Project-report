package lk.leoclub.clubprojects.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import lk.leoclub.clubprojects.model.CatalogItem;

public interface CatalogItemRepository extends JpaRepository<CatalogItem, String> {

    List<CatalogItem> findByKindOrderByPositionAsc(String kind);

    boolean existsByKindAndLabelIgnoreCase(String kind, String label);

    long countByKind(String kind);
}
