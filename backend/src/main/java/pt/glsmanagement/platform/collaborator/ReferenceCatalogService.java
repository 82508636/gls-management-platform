package pt.glsmanagement.platform.collaborator;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

import static pt.glsmanagement.platform.collaborator.ReferenceCatalogDtos.*;

@Service
class ReferenceCatalogService {
    private final AccountProfileRepository profiles;
    private final ProfessionalCategoryRepository categories;

    ReferenceCatalogService(AccountProfileRepository profiles, ProfessionalCategoryRepository categories) {
        this.profiles = profiles;
        this.categories = categories;
    }

    @Transactional(readOnly = true)
    List<Response> profiles() {
        return profiles.findAll(Sort.by("designation")).stream().map(Response::from).toList();
    }

    @Transactional
    Response createProfile(CreateRequest request, String actor) {
        var id = normalizeId(request.id());
        var designation = normalizeDesignation(request.designation());
        if (profiles.existsById(id) || profiles.existsByDesignationIgnoreCase(designation)) throw duplicate();
        return Response.from(profiles.save(AccountProfile.create(id, designation, actor)));
    }

    @Transactional
    Response updateProfile(String rawId, UpdateRequest request, String actor) {
        var id = normalizeId(rawId);
        var designation = normalizeDesignation(request.designation());
        if (profiles.existsByDesignationIgnoreCaseAndIdNot(designation, id)) throw duplicate();
        var profile = profiles.findById(id).orElseThrow(ReferenceCatalogService::notFound);
        profile.update(designation, actor);
        return Response.from(profile);
    }

    @Transactional
    Response setProfileStatus(String rawId, boolean active, String actor) {
        var profile = profiles.findById(normalizeId(rawId)).orElseThrow(ReferenceCatalogService::notFound);
        profile.setActive(active, actor);
        return Response.from(profile);
    }

    @Transactional(readOnly = true)
    List<Response> categories() {
        return categories.findAll(Sort.by("designation")).stream().map(Response::from).toList();
    }

    @Transactional
    Response createCategory(CreateRequest request, String actor) {
        var id = normalizeId(request.id());
        var designation = normalizeDesignation(request.designation());
        if (categories.existsById(id) || categories.existsByDesignationIgnoreCase(designation)) throw duplicate();
        return Response.from(categories.save(ProfessionalCategory.create(id, designation, actor)));
    }

    @Transactional
    Response updateCategory(String rawId, UpdateRequest request, String actor) {
        var id = normalizeId(rawId);
        var designation = normalizeDesignation(request.designation());
        if (categories.existsByDesignationIgnoreCaseAndIdNot(designation, id)) throw duplicate();
        var category = categories.findById(id).orElseThrow(ReferenceCatalogService::notFound);
        category.update(designation, actor);
        return Response.from(category);
    }

    @Transactional
    Response setCategoryStatus(String rawId, boolean active, String actor) {
        var category = categories.findById(normalizeId(rawId)).orElseThrow(ReferenceCatalogService::notFound);
        category.setActive(active, actor);
        return Response.from(category);
    }

    private static String normalizeId(String value) { return value.trim().toUpperCase(java.util.Locale.ROOT); }
    private static String normalizeDesignation(String value) { return value.trim().replaceAll("\\s+", " "); }
    private static ReferenceCatalogException duplicate() { return new ReferenceCatalogException(ReferenceCatalogException.Reason.DUPLICATE); }
    private static ReferenceCatalogException notFound() { return new ReferenceCatalogException(ReferenceCatalogException.Reason.NOT_FOUND); }
}
