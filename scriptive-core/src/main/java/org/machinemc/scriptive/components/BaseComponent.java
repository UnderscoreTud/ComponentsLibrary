package org.machinemc.scriptive.components;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import org.machinemc.scriptive.Contents;
import org.machinemc.scriptive.events.ClickEvent;
import org.machinemc.scriptive.events.HoverEvent;
import org.machinemc.scriptive.style.ChatStyle;
import org.machinemc.scriptive.style.Colour;
import org.machinemc.scriptive.style.TextFormat;

import java.util.*;
import java.util.function.Consumer;

/**
 * Shared component implementation.
 * <p>
 * Can be extended to create custom component implementations.
 *
 * @see TextComponent
 * @see TranslationComponent
 * @see KeybindComponent
 */
public abstract non-sealed class BaseComponent implements Component {

    private final List<Component> siblings;
    private TextFormat textFormat;
    private @Nullable String insertion;
    private @Nullable ClickEvent clickEvent;
    private @Nullable HoverEvent<?> hoverEvent;

    protected BaseComponent() {
        this(new ArrayList<>(), new TextFormat());
    }

    protected BaseComponent(List<Component> siblings, TextFormat textFormat) {
        this.siblings = siblings;
        this.textFormat = textFormat;
    }

    @Override
    public TextFormat getTextFormat() {
        return textFormat;
    }

    @Override
    public void setTextFormat(TextFormat textFormat) {
        this.textFormat = Objects.requireNonNull(textFormat, "textFormat");
    }

    @Override
    public Optional<Colour> getColor() {
        return textFormat.getColor();
    }

    @Override
    public void setColor(@Nullable Colour color) {
        textFormat.setColor(color);
    }

    @Override
    public Optional<Boolean> isBold() {
        return textFormat.getStyle(ChatStyle.BOLD);
    }

    @Override
    public void setBold(@Nullable Boolean bold) {
        textFormat.setStyle(ChatStyle.BOLD, bold);
    }

    @Override
    public Optional<Boolean> isObfuscated() {
        return textFormat.getStyle(ChatStyle.OBFUSCATED);
    }

    @Override
    public void setObfuscated(@Nullable Boolean obfuscated) {
        textFormat.setStyle(ChatStyle.OBFUSCATED, obfuscated);
    }

    @Override
    public Optional<Boolean> isItalic() {
        return textFormat.getStyle(ChatStyle.ITALIC);
    }

    @Override
    public void setItalic(@Nullable Boolean italic) {
        textFormat.setStyle(ChatStyle.ITALIC, italic);
    }

    @Override
    public Optional<Boolean> isUnderlined() {
        return textFormat.getStyle(ChatStyle.UNDERLINED);
    }

    @Override
    public void setUnderlined(@Nullable Boolean underlined) {
        textFormat.setStyle(ChatStyle.UNDERLINED, underlined);
    }

    @Override
    public Optional<Boolean> isStrikethrough() {
        return textFormat.getStyle(ChatStyle.STRIKETHROUGH);
    }

    @Override
    public void setStrikethrough(@Nullable Boolean strikethrough) {
        textFormat.setStyle(ChatStyle.STRIKETHROUGH, strikethrough);
    }

    @Override
    public Optional<String> getFont() {
        return textFormat.getFont();
    }

    @Override
    public void setFont(@Nullable String font) {
        textFormat.setFont(font);
    }

    @Override
    public Optional<String> getInsertion() {
        return Optional.ofNullable(insertion);
    }

    @Override
    public void setInsertion(@Nullable String insertion) {
        this.insertion = insertion;
    }

    @Override
    public Optional<ClickEvent> getClickEvent() {
        return Optional.ofNullable(clickEvent);
    }

    @Override
    public void setClickEvent(@Nullable ClickEvent clickEvent) {
        this.clickEvent = clickEvent;
    }

    @Override
    public Optional<HoverEvent<?>> getHoverEvent() {
        return Optional.ofNullable(hoverEvent);
    }

    @Override
    public void setHoverEvent(@Nullable HoverEvent<?> hoverEvent) {
        this.hoverEvent = hoverEvent;
    }

    @Override
    public @UnmodifiableView List<Component> getSiblings() {
        return Collections.unmodifiableList(siblings);
    }

    @Override
    public BaseComponent append(String literal) {
        return (BaseComponent) Component.super.append(literal);
    }

    @Override
    public BaseComponent append(String literal, TextFormat textFormat) {
        return (BaseComponent) Component.super.append(literal, textFormat);
    }

    @Override
    public BaseComponent append(Component component) {
        siblings.add(component.clone());
        return this;
    }

    @Override
    public void clearSiblings() {
        siblings.clear();
    }

    @Override
    public void inheritFrom(Component parent) {
        textFormat.inheritFrom(parent.getTextFormat());
        getInsertion().ifPresentOrElse(k -> {}, () -> setInsertion(parent.getInsertion().orElse(null)));
        getClickEvent().ifPresentOrElse(k -> {}, () -> setClickEvent(parent.getClickEvent().orElse(null)));
        getHoverEvent().ifPresentOrElse(k -> {}, () -> setHoverEvent(parent.getHoverEvent().orElse(null)));
    }

    @Override
    public void merge(Component other) {
        if (other.hasSiblings()) {
            clearSiblings();
            other.getSiblings().forEach(sibling -> append(sibling.clone()));
        }

        textFormat.merge(other.getTextFormat());

        other.getInsertion().ifPresent(this::setInsertion);
        other.getClickEvent().ifPresent(this::setClickEvent);
        other.getHoverEvent().ifPresent(this::setHoverEvent);
    }

    @Override
    public String toLegacyString() {
        List<Component> components = toFlatList();
        StringBuilder builder = new StringBuilder();
        for (Component component : components) {
            TextFormat format = component.getTextFormat();
            format.getColor().ifPresent(color -> {
                if (color.isDefaultColor()) {
                    builder.append(color);
                } else {
                    builder.append("&x&").append(String.join("&", color.getHexString().split("")));
                }
            });
            for (ChatStyle style : format.getStyles(true))
                builder.append(style);
            builder.append(component.getString());
        }
        return builder.toString();
    }

    @Override
    public List<Component> toFlatList() {
        List<Component> components = new LinkedList<>();
        addSeparatedComponents(clone(), components::add);
        return components;
    }

    private static void addSeparatedComponents(Component parent, Consumer<Component> consumer) {
        consumer.accept(parent);
        for (Component child : parent.getSiblings()) {
            child.inheritFrom(parent);
            addSeparatedComponents(child, consumer);
        }
        parent.clearSiblings();
    }

    @Override
    public abstract BaseComponent clone();

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = new HashMap<>(textFormat.asMap());
        getInsertion().ifPresent(insertion -> map.put("insertion", insertion));
        getClickEvent().ifPresent(clickEvent -> map.put("clickEvent", clickEvent.asMap()));
        getHoverEvent().ifPresent(hoverEvent -> map.put("hoverEvent", hoverEvent.asMap()));
        if (hasSiblings())
            map.put("extra", getSiblings().stream().map(Contents::asMap).toArray(Map[]::new));
        return map;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseComponent that)) return false;
        return asMap().equals(that.asMap());
    }

    @Override
    public int hashCode() {
        return asMap().hashCode();
    }

}
