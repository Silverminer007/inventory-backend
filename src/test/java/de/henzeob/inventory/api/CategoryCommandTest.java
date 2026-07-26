package de.henzeob.inventory.api;

import de.henzeob.inventory.model.entity.Category;
import de.henzeob.inventory.repository.CategoryRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static de.henzeob.inventory.support.SyncTestSupport.FUTURE;
import static de.henzeob.inventory.support.SyncTestSupport.PAST;
import static de.henzeob.inventory.support.SyncTestSupport.applyExpectStatus;
import static de.henzeob.inventory.support.SyncTestSupport.applyOk;
import static de.henzeob.inventory.support.SyncTestSupport.currentHead;
import static de.henzeob.inventory.support.SyncTestSupport.payload;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** "Category Tests" section of DOMAIN-RULES.md. */
@QuarkusTest
public class CategoryCommandTest {

    @Inject
    CategoryRepository categoryRepository;

    @Test
    public void create_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        applyOk(head, "CATEGORY_CREATE",
                payload("id", id.toString(), "name", "Electronics", "shortcode", "ELEC",
                        "description", "Gadgets", "hue", 120, "created_at", PAST));

        Category category = categoryRepository.findByIdOptional(id).orElseThrow();
        assertEquals("Electronics", category.name);
        assertEquals("ELEC", category.shortCode);
        assertEquals("Gadgets", category.description);
        assertEquals(120, category.hue);
    }

    @Test
    public void create_thenDelete_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CATEGORY_CREATE", payload("id", id.toString(), "name", "Temp Cat", "shortcode", "TMP1", "created_at", PAST));
        applyOk(head, "CATEGORY_DELETE", payload("id", id.toString()));

        assertTrue(categoryRepository.findByIdOptional(id).isEmpty());
    }

    @Test
    public void create_thenUpdate_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CATEGORY_CREATE", payload("id", id.toString(), "name", "Books", "shortcode", "BOOK", "created_at", PAST));
        applyOk(head, "CATEGORY_UPDATE", payload("id", id.toString(), "name", "Books & Media"));

        Category category = categoryRepository.findByIdOptional(id).orElseThrow();
        assertEquals("Books & Media", category.name);
    }

    @Test
    public void create_update_delete_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CATEGORY_CREATE", payload("id", id.toString(), "name", "Toys", "shortcode", "TOYS", "created_at", PAST));
        head = applyOk(head, "CATEGORY_UPDATE", payload("id", id.toString(), "name", "Toys & Games"));
        applyOk(head, "CATEGORY_DELETE", payload("id", id.toString()));

        assertTrue(categoryRepository.findByIdOptional(id).isEmpty());
    }

    @Test
    public void updateName_thenUpdateNameAgain_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CATEGORY_CREATE", payload("id", id.toString(), "name", "First", "shortcode", "FRST", "created_at", PAST));
        head = applyOk(head, "CATEGORY_UPDATE", payload("id", id.toString(), "name", "Second"));
        applyOk(head, "CATEGORY_UPDATE", payload("id", id.toString(), "name", "Third"));

        assertEquals("Third", categoryRepository.findByIdOptional(id).orElseThrow().name);
    }

    @Test
    public void updateName_thenUpdateDescription_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CATEGORY_CREATE", payload("id", id.toString(), "name", "First", "shortcode", "SHRT", "created_at", PAST));
        head = applyOk(head, "CATEGORY_UPDATE", payload("id", id.toString(), "name", "Renamed"));
        applyOk(head, "CATEGORY_UPDATE", payload("id", id.toString(), "description", "New description"));

        Category category = categoryRepository.findByIdOptional(id).orElseThrow();
        assertEquals("Renamed", category.name);
        assertEquals("New description", category.description);
    }

    @Test
    public void create_withoutId_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "CATEGORY_CREATE",
                payload("name", "No Id", "shortcode", "NOID", "created_at", PAST), 400);
    }

    @Test
    public void create_withoutName_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "CATEGORY_CREATE",
                payload("id", UUID.randomUUID().toString(), "shortcode", "NONM", "created_at", PAST), 400);
    }

    @Test
    public void create_withBlankName_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "CATEGORY_CREATE",
                payload("id", UUID.randomUUID().toString(), "name", "   ", "shortcode", "BLNK", "created_at", PAST), 400);
    }

    @Test
    public void create_withNameTwoChars_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "CATEGORY_CREATE",
                payload("id", UUID.randomUUID().toString(), "name", "Ab", "shortcode", "SHRT", "created_at", PAST), 400);
    }

    @Test
    public void create_withoutShortcode_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "CATEGORY_CREATE",
                payload("id", UUID.randomUUID().toString(), "name", "No Shortcode", "created_at", PAST), 400);
    }

    @Test
    public void create_withBlankShortcode_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "CATEGORY_CREATE",
                payload("id", UUID.randomUUID().toString(), "name", "Blank Shortcode", "shortcode", "   ", "created_at", PAST), 400);
    }

    @Test
    public void create_withShortcodeTwoChars_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        applyOk(head, "CATEGORY_CREATE",
                payload("id", id.toString(), "name", "Two Char Code", "shortcode", "AB", "created_at", PAST));
        assertEquals("AB", categoryRepository.findByIdOptional(id).orElseThrow().shortCode);
    }

    @Test
    public void create_withShortcodeFiveChars_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "CATEGORY_CREATE",
                payload("id", UUID.randomUUID().toString(), "name", "Five Char Code", "shortcode", "ABCDE", "created_at", PAST), 400);
    }

    @Test
    public void create_withInvalidUuidId_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "CATEGORY_CREATE",
                payload("id", "not-a-uuid", "name", "Invalid Id", "shortcode", "INVD", "created_at", PAST), 400);
    }

    @Test
    public void create_withCreatedAtInFuture_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "CATEGORY_CREATE",
                payload("id", UUID.randomUUID().toString(), "name", "Future Cat", "shortcode", "FUTR", "created_at", FUTURE), 400);
    }

    @Test
    public void create_withCreatedAtInPast_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        applyOk(head, "CATEGORY_CREATE",
                payload("id", id.toString(), "name", "Past Cat", "shortcode", "PAST", "created_at", PAST));
        assertTrue(categoryRepository.findByIdOptional(id).isPresent());
    }

    @Test
    public void update_withoutId_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "CATEGORY_UPDATE", payload("name", "No Id Update"), 400);
    }

    @Test
    public void update_withoutDescription_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CATEGORY_CREATE",
                payload("id", id.toString(), "name", "Desc Cat", "shortcode", "DESC", "description", "orig", "created_at", PAST));
        applyOk(head, "CATEGORY_UPDATE", payload("id", id.toString(), "name", "Desc Cat 2"));

        Category category = categoryRepository.findByIdOptional(id).orElseThrow();
        assertEquals("orig", category.description);
    }

    @Test
    public void update_withoutHue_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CATEGORY_CREATE",
                payload("id", id.toString(), "name", "Hue Cat", "shortcode", "HUEC", "hue", 42, "created_at", PAST));
        applyOk(head, "CATEGORY_UPDATE", payload("id", id.toString(), "name", "Hue Cat 2"));

        Category category = categoryRepository.findByIdOptional(id).orElseThrow();
        assertEquals(42, category.hue);
    }

    @Test
    public void update_onlyId_leavesSnapshotUnchanged() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CATEGORY_CREATE",
                payload("id", id.toString(), "name", "Unchanged", "shortcode", "UNCH", "description", "d", "hue", 7, "created_at", PAST));
        applyOk(head, "CATEGORY_UPDATE", payload("id", id.toString()));

        Category category = categoryRepository.findByIdOptional(id).orElseThrow();
        assertEquals("Unchanged", category.name);
        assertEquals("UNCH", category.shortCode);
        assertEquals("d", category.description);
        assertEquals(7, category.hue);
    }

    @Test
    public void create_withHueZero_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        applyOk(head, "CATEGORY_CREATE",
                payload("id", id.toString(), "name", "Hue Zero", "shortcode", "HZR", "hue", 0, "created_at", PAST));
        assertEquals(0, categoryRepository.findByIdOptional(id).orElseThrow().hue);
    }

    @Test
    public void create_withHue360_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        applyOk(head, "CATEGORY_CREATE",
                payload("id", id.toString(), "name", "Hue 360", "shortcode", "H360", "hue", 360, "created_at", PAST));
        assertEquals(360, categoryRepository.findByIdOptional(id).orElseThrow().hue);
    }

    @Test
    public void create_withHue361_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "CATEGORY_CREATE",
                payload("id", UUID.randomUUID().toString(), "name", "Hue 361", "shortcode", "H361", "hue", 361, "created_at", PAST), 400);
    }

    @Test
    public void create_withHueMinusOne_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "CATEGORY_CREATE",
                payload("id", UUID.randomUUID().toString(), "name", "Hue -1", "shortcode", "HM1", "hue", -1, "created_at", PAST), 400);
    }

    @Test
    public void create_withoutHue_staysOptional() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        applyOk(head, "CATEGORY_CREATE",
                payload("id", id.toString(), "name", "No Hue", "shortcode", "NHUE", "created_at", PAST));
        assertNull(categoryRepository.findByIdOptional(id).orElseThrow().hue);
    }

    @Test
    public void update_withoutName_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CATEGORY_CREATE", payload("id", id.toString(), "name", "Stable Name", "shortcode", "STBL", "created_at", PAST));
        applyOk(head, "CATEGORY_UPDATE", payload("id", id.toString(), "description", "changed"));

        assertEquals("Stable Name", categoryRepository.findByIdOptional(id).orElseThrow().name);
    }

    @Test
    public void update_withBlankName_fails() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CATEGORY_CREATE", payload("id", id.toString(), "name", "Valid", "shortcode", "VLD1", "created_at", PAST));
        applyExpectStatus(head, "CATEGORY_UPDATE", payload("id", id.toString(), "name", "   "), 400);
    }

    @Test
    public void update_withNameTwoChars_fails() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CATEGORY_CREATE", payload("id", id.toString(), "name", "Valid", "shortcode", "VLD2", "created_at", PAST));
        applyExpectStatus(head, "CATEGORY_UPDATE", payload("id", id.toString(), "name", "Ab"), 400);
    }

    @Test
    public void update_withoutShortcode_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CATEGORY_CREATE", payload("id", id.toString(), "name", "Keep Code", "shortcode", "KEEP", "created_at", PAST));
        applyOk(head, "CATEGORY_UPDATE", payload("id", id.toString(), "name", "Keep Code 2"));

        assertEquals("KEEP", categoryRepository.findByIdOptional(id).orElseThrow().shortCode);
    }

    @Test
    public void update_withBlankShortcode_fails() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CATEGORY_CREATE", payload("id", id.toString(), "name", "Valid", "shortcode", "VLD3", "created_at", PAST));
        applyExpectStatus(head, "CATEGORY_UPDATE", payload("id", id.toString(), "shortcode", "   "), 400);
    }

    @Test
    public void update_withShortcodeTwoChars_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CATEGORY_CREATE", payload("id", id.toString(), "name", "Valid", "shortcode", "VLD4", "created_at", PAST));
        applyOk(head, "CATEGORY_UPDATE", payload("id", id.toString(), "shortcode", "XY"));

        assertEquals("XY", categoryRepository.findByIdOptional(id).orElseThrow().shortCode);
    }

    @Test
    public void update_withShortcodeFiveChars_fails() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CATEGORY_CREATE", payload("id", id.toString(), "name", "Valid", "shortcode", "VLD5", "created_at", PAST));
        applyExpectStatus(head, "CATEGORY_UPDATE", payload("id", id.toString(), "shortcode", "ABCDE"), 400);
    }

    @Test
    public void update_withInvalidUuidId_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "CATEGORY_UPDATE", payload("id", "not-a-uuid", "name", "X"), 400);
    }

    @Test
    public void update_withCreatedAtInFuture_fails() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CATEGORY_CREATE", payload("id", id.toString(), "name", "Valid", "shortcode", "VLD6", "created_at", PAST));
        applyExpectStatus(head, "CATEGORY_UPDATE", payload("id", id.toString(), "created_at", FUTURE), 400);
    }

    @Test
    public void update_withCreatedAtInPast_fails() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CATEGORY_CREATE", payload("id", id.toString(), "name", "Valid", "shortcode", "VLD7", "created_at", PAST));
        applyExpectStatus(head, "CATEGORY_UPDATE", payload("id", id.toString(), "created_at", PAST), 400);
    }

    @Test
    public void update_withEmptyCreatedAt_fails() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CATEGORY_CREATE", payload("id", id.toString(), "name", "Valid", "shortcode", "VLD8", "created_at", PAST));
        applyExpectStatus(head, "CATEGORY_UPDATE", payload("id", id.toString(), "created_at", ""), 400);
    }

    @Test
    public void update_withHueZero_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CATEGORY_CREATE", payload("id", id.toString(), "name", "Valid", "shortcode", "VLD9", "created_at", PAST));
        applyOk(head, "CATEGORY_UPDATE", payload("id", id.toString(), "hue", 0));

        assertEquals(0, categoryRepository.findByIdOptional(id).orElseThrow().hue);
    }

    @Test
    public void update_withHue360_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CATEGORY_CREATE", payload("id", id.toString(), "name", "Valid", "shortcode", "VL10", "created_at", PAST));
        applyOk(head, "CATEGORY_UPDATE", payload("id", id.toString(), "hue", 360));

        assertEquals(360, categoryRepository.findByIdOptional(id).orElseThrow().hue);
    }

    @Test
    public void update_withHue361_fails() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CATEGORY_CREATE", payload("id", id.toString(), "name", "Valid", "shortcode", "VL11", "created_at", PAST));
        applyExpectStatus(head, "CATEGORY_UPDATE", payload("id", id.toString(), "hue", 361), 400);
    }

    @Test
    public void update_withHueMinusOne_fails() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        head = applyOk(head, "CATEGORY_CREATE", payload("id", id.toString(), "name", "Valid", "shortcode", "VL12", "created_at", PAST));
        applyExpectStatus(head, "CATEGORY_UPDATE", payload("id", id.toString(), "hue", -1), 400);
    }

    @Test
    public void delete_withoutId_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "CATEGORY_DELETE", payload(), 400);
    }

    @Test
    public void delete_withNonExistentId_fails() {
        UUID head = currentHead();
        applyExpectStatus(head, "CATEGORY_DELETE", payload("id", UUID.randomUUID().toString()), 400);
    }

    @Test
    public void create_withOnlyRequiredFields_succeeds() {
        UUID head = currentHead();
        UUID id = UUID.randomUUID();
        applyOk(head, "CATEGORY_CREATE",
                payload("id", id.toString(), "name", "Minimal", "shortcode", "MIN1", "created_at", PAST));

        Category category = categoryRepository.findByIdOptional(id).orElseThrow();
        assertEquals("Minimal", category.name);
        assertNull(category.description);
        assertNull(category.hue);
    }
}
