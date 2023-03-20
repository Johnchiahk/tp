package arb.model.tag;

import static arb.commons.util.CollectionUtil.requireAllNonNull;
import static java.util.Objects.requireNonNull;

import java.util.Iterator;
import java.util.List;

import arb.model.client.Client;
import arb.model.project.Project;
import arb.model.tag.exceptions.DuplicateTagMappingException;
import arb.model.tag.exceptions.TagMappingNotFoundException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * A list of tag mappings that enforces uniqueness between its elements and does not allow nulls.
 * A tag mapping is considered unique by comparing using {@code TagMapping#isSameTagMapping(TagMapping)}. As such,
 * adding and updating of tag mappings uses TagMapping#isSameTagMapping(TagMapping) for equality so as to ensure that
 * the tag mapping being added or updated is unique in terms of identity in the UniqueTagMappingList. However, the
 * removal of a tag mapping uses TagMapping#equals(Object) so as to ensure that the tag mapping with exactly the same
 * fields will be removed.
 *
 * Supports a minimal set of list operations.
 *
 * @see TagMapping#isSameTagMapping(TagMapping)
 */
public class UniqueTagMappingList implements Iterable<TagMapping> {

    private final ObservableList<TagMapping> internalList = FXCollections.observableArrayList();
    private final ObservableList<TagMapping> internalUnmodifiableList =
            FXCollections.unmodifiableObservableList(internalList);

    /**
     * Returns true if the list contains an equivalent tag mapping as the given argument.
     */
    public boolean contains(TagMapping toCheck) {
        requireNonNull(toCheck);
        return internalList.stream().anyMatch(toCheck::isSameTagMapping);
    }

    /**
     * Returns true if the list contains a tag mapping wrapping around the given tag.
     */
    public boolean contains(Tag tag) {
        requireNonNull(tag);
        return internalList.stream().anyMatch(t -> t.isSameTagMapping(tag));
    }

    /**
     * Adds a tag mapping to the list.
     * The tag mapping must not already exist in the list.
     */
    public void add(TagMapping toAdd) {
        requireNonNull(toAdd);
        if (contains(toAdd)) {
            throw new DuplicateTagMappingException();
        }
        internalList.add(toAdd);
    }

    /**
     * Replaces the tag mapping {@code target} in the list with {@code editedTagMapping}.
     * {@code target} must exist in the list.
     * The tag mapping identity of {@code editedTagMapping} must not be the same as another existing tag mapping
     * in the list.
     */
    public void setTagMapping(TagMapping target, TagMapping editedTagMapping) {
        requireAllNonNull(target, editedTagMapping);

        int index = internalList.indexOf(target);
        if (index == -1) {
            throw new TagMappingNotFoundException();
        }

        if (!target.isSameTagMapping(editedTagMapping) && contains(editedTagMapping)) {
            throw new DuplicateTagMappingException();
        }

        internalList.set(index, editedTagMapping);
    }

    /**
     * Removes the equivalent tag mapping from the list.
     * The tag mapping must exist in the list.
     */
    public void remove(TagMapping toRemove) {
        requireNonNull(toRemove);
        if (!internalList.remove(toRemove)) {
            throw new TagMappingNotFoundException();
        }
    }

    public void setTagMappings(UniqueTagMappingList replacement) {
        requireNonNull(replacement);
        internalList.setAll(replacement.internalList);
    }

    /**
     * Replaces the contents of this list with {@code tagMappings}.
     * {@code tagMappings} must not contain duplicate tag mappings.
     */
    public void setTagMappings(List<TagMapping> tagMappings) {
        requireAllNonNull(tagMappings);
        if (!tagMappingsAreUnique(tagMappings)) {
            throw new DuplicateTagMappingException();
        }

        internalList.setAll(tagMappings);
    }

    /**
     * Returns the backing list as an unmodifiable {@code ObservableList}.
     */
    public ObservableList<TagMapping> asUnmodifiableObservableList() {
        return internalUnmodifiableList;
    }

    /**
     * Updates the list with the tag mappings from the newly added {@code client}.
     */
    public void addClientTags(Client client) {
        for (Tag t : client.getTags()) {
            addClientTag(t);
        }
    }

    /**
     * Updates the list with the tag mappings from the newly added {@code project}.
     */
    public void addProjectTags(Project project) {
        for (Tag t : project.getTags()) {
            addProjectTag(t);
        }
    }

    /**
     * Updates the tag mappings in the list to match {@code editedClient}.
     */
    public void editClientTags(Client originalClient, Client editedClient) {
        for (Tag t : originalClient.getTags()) {
            removeClientTag(t);
        }

        for (Tag t : editedClient.getTags()) {
            addClientTag(t);
        }
    }

    /**
     * Updates the tag mappings in the list to match {@code editedProject}.
     */
    public void editProjectTags(Project originalProject, Project editedProject) {
        for (Tag t : originalProject.getTags()) {
            removeProjectTag(t);
        }

        for (Tag t : editedProject.getTags()) {
            addProjectTag(t);
        }
    }

    /**
     * Updates the tag mappings in the list to remove mappings for {@code deletedClient}.
     */
    public void deleteClientTags(Client deletedClient) {
        for (Tag t : deletedClient.getTags()) {
            removeClientTag(t);
        }
    }

    /**
     * Updates the tag mappings in the list to remove mappings for {@code deletedProject}.
     */
    public void deleteProjectTags(Project deletedProject) {
        for (Tag t : deletedProject.getTags()) {
            removeProjectTag(t);
        }
    }

    private void addClientTag(Tag tag) {
        if (!contains(tag)) {
            add(new TagMapping(tag));
        }

        TagMapping mapping = getTagMapping(tag);
        mapping.tagClient();
        setTagMapping(mapping, mapping);
    }

    private void removeClientTag(Tag tag) {
        TagMapping mapping = getTagMapping(tag);
        mapping.untagClient();

        setTagMapping(mapping, mapping);

        if (mapping.noObjectsTagged()) {
            remove(mapping);
        }
    }

    private void addProjectTag(Tag tag) {
        if (!contains(tag)) {
            add(new TagMapping(tag));
        }

        TagMapping mapping = getTagMapping(tag);
        mapping.tagProject();
        setTagMapping(mapping, mapping);
    }

    private void removeProjectTag(Tag tag) {
        TagMapping mapping = getTagMapping(tag);
        mapping.untagProject();

        setTagMapping(mapping, mapping);

        if (mapping.noObjectsTagged()) {
            remove(mapping);
        }
    }

    private TagMapping getTagMapping(Tag tag) {
        Iterator<TagMapping> iterator = iterator();
        while (iterator.hasNext()) {
            TagMapping mapping = iterator.next();
            if (mapping.isSameTagMapping(tag)) {
                return mapping;
            }
        }

        throw new TagMappingNotFoundException();
    }

    @Override
    public Iterator<TagMapping> iterator() {
        return internalList.iterator();
    }

    @Override
    public boolean equals(Object other) {
        return other == this // short circuit if same object
                || (other instanceof UniqueTagMappingList // instanceof handles nulls
                        && internalList.equals(((UniqueTagMappingList) other).internalList));
    }

    @Override
    public int hashCode() {
        return internalList.hashCode();
    }

    /**
     * Returns true if {@code tagMappings} contains only unique tag mappings.
     */
    private boolean tagMappingsAreUnique(List<TagMapping> tagMappings) {
        for (int i = 0; i < tagMappings.size() - 1; i++) {
            for (int j = i + 1; j < tagMappings.size(); j++) {
                if (tagMappings.get(i).isSameTagMapping(tagMappings.get(j))) {
                    return false;
                }
            }
        }
        return true;
    }
}
