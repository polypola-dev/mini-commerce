/* @ds-bundle: {"format":3,"namespace":"MiniCommerceDesignSystem_f7d34b","components":[{"name":"Avatar","sourcePath":"components/core/Avatar.jsx"},{"name":"Badge","sourcePath":"components/core/Badge.jsx"},{"name":"Button","sourcePath":"components/core/Button.jsx"},{"name":"IconButton","sourcePath":"components/core/IconButton.jsx"},{"name":"Input","sourcePath":"components/core/Input.jsx"},{"name":"StarRating","sourcePath":"components/core/StarRating.jsx"},{"name":"PropertyCard","sourcePath":"components/marketplace/PropertyCard.jsx"},{"name":"RatingDisplay","sourcePath":"components/marketplace/RatingDisplay.jsx"},{"name":"ReservationCard","sourcePath":"components/marketplace/ReservationCard.jsx"},{"name":"SearchBar","sourcePath":"components/marketplace/SearchBar.jsx"},{"name":"TopNav","sourcePath":"components/marketplace/TopNav.jsx"}],"sourceHashes":{"components/core/Avatar.jsx":"3f0d739e2777","components/core/Badge.jsx":"7b90a2c591f5","components/core/Button.jsx":"c1b665a05c9d","components/core/IconButton.jsx":"7fe4fe128f2c","components/core/Input.jsx":"10274336846a","components/core/StarRating.jsx":"5e9d36ea470e","components/marketplace/PropertyCard.jsx":"eed86c89aae0","components/marketplace/RatingDisplay.jsx":"6b3f82c86c21","components/marketplace/ReservationCard.jsx":"ace97c813555","components/marketplace/SearchBar.jsx":"1bec25d46008","components/marketplace/TopNav.jsx":"a8f3693a946e","ui_kits/marketplace/Home.jsx":"5c847c4e50f7","ui_kits/marketplace/ListingDetail.jsx":"4a1c48fb93a0","ui_kits/marketplace/SearchResults.jsx":"c17612684db3","ui_kits/marketplace/data.js":"28697e0dbfa7"},"inlinedExternals":[],"unexposedExports":[]} */

(() => {

const __ds_ns = (window.MiniCommerceDesignSystem_f7d34b = window.MiniCommerceDesignSystem_f7d34b || {});

const __ds_scope = {};

(__ds_ns.__errors = __ds_ns.__errors || []);

// components/core/Avatar.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Mini-Commerce Avatar — circular user image with initials fallback.
 * Optional Superhost ring badge for host surfaces.
 */
function Avatar({
  src,
  name = "",
  size = 56,
  superhost = false,
  style,
  ...rest
}) {
  const initials = name.split(" ").map(p => p[0]).filter(Boolean).slice(0, 2).join("").toUpperCase();
  return /*#__PURE__*/React.createElement("span", _extends({
    style: {
      position: "relative",
      display: "inline-flex",
      flex: "none",
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("span", {
    style: {
      width: size,
      height: size,
      borderRadius: "var(--radius-full)",
      overflow: "hidden",
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      background: "var(--color-surface-strong)",
      color: "var(--color-ink)",
      fontFamily: "var(--font-sans)",
      fontWeight: "var(--weight-semibold)",
      fontSize: Math.round(size * 0.36)
    }
  }, src ? /*#__PURE__*/React.createElement("img", {
    src: src,
    alt: name,
    style: {
      width: "100%",
      height: "100%",
      objectFit: "cover"
    }
  }) : initials || "?"), superhost && /*#__PURE__*/React.createElement("span", {
    style: {
      position: "absolute",
      right: -2,
      bottom: -2,
      width: Math.round(size * 0.34),
      height: Math.round(size * 0.34),
      borderRadius: "var(--radius-full)",
      background: "var(--color-primary)",
      border: "2px solid var(--color-canvas)",
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      color: "var(--color-on-primary)",
      fontSize: Math.round(size * 0.18)
    },
    "aria-label": "Superhost"
  }, "\u2605"));
}
Object.assign(__ds_scope, { Avatar });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/Avatar.jsx", error: String((e && e.message) || e) }); }

// components/core/Badge.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Mini-Commerce Badge — small floating labels over photography.
 * `favorite` = white pill with the system shadow ("Guest favorite").
 * `new` = tiny uppercase "NEW" tag. `neutral` = plain ink-on-white pill.
 */
function Badge({
  variant = "favorite",
  children,
  style,
  ...rest
}) {
  const base = {
    fontFamily: "var(--font-sans)",
    display: "inline-flex",
    alignItems: "center",
    borderRadius: "var(--radius-full)",
    whiteSpace: "nowrap"
  };
  const variants = {
    favorite: {
      background: "var(--color-canvas)",
      color: "var(--color-ink)",
      fontSize: "var(--type-badge-size)",
      fontWeight: "var(--type-badge-weight)",
      padding: "6px 10px",
      boxShadow: "var(--shadow-card)"
    },
    new: {
      background: "var(--color-canvas)",
      color: "var(--color-ink)",
      fontSize: "var(--type-uppercase-tag-size)",
      fontWeight: "var(--type-uppercase-tag-weight)",
      letterSpacing: "var(--type-uppercase-tag-tracking)",
      textTransform: "uppercase",
      padding: "3px 6px",
      boxShadow: "var(--shadow-card)"
    },
    neutral: {
      background: "var(--color-surface-strong)",
      color: "var(--color-ink)",
      fontSize: "var(--type-badge-size)",
      fontWeight: "var(--type-badge-weight)",
      padding: "6px 10px"
    }
  };
  return /*#__PURE__*/React.createElement("span", _extends({
    style: {
      ...base,
      ...variants[variant],
      ...style
    }
  }, rest), children ?? (variant === "new" ? "New" : "Guest favorite"));
}
Object.assign(__ds_scope, { Badge });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/Badge.jsx", error: String((e && e.message) || e) }); }

// components/core/Button.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Mini-Commerce Button — the system's primary action surface.
 * Rausch fill by default; secondary (outlined), tertiary (text), and a
 * fully-rounded pill variant. 8px radius, 48px tall, modest weight-500 label.
 */
function Button({
  variant = "primary",
  size = "md",
  disabled = false,
  fullWidth = false,
  children,
  style,
  ...rest
}) {
  const base = {
    fontFamily: "var(--font-sans)",
    fontWeight: "var(--weight-medium)",
    border: "1px solid transparent",
    cursor: disabled ? "not-allowed" : "pointer",
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    gap: "var(--space-sm)",
    width: fullWidth ? "100%" : "auto",
    transition: "background 120ms ease, border-color 120ms ease, opacity 120ms ease",
    lineHeight: 1,
    whiteSpace: "nowrap"
  };
  const sizes = {
    md: {
      fontSize: "var(--type-button-md-size)",
      minHeight: "48px",
      padding: "0 var(--space-lg)",
      borderRadius: "var(--radius-sm)"
    },
    sm: {
      fontSize: "var(--type-button-sm-size)",
      minHeight: "40px",
      padding: "0 var(--space-base)",
      borderRadius: "var(--radius-sm)"
    },
    pill: {
      fontSize: "var(--type-button-sm-size)",
      minHeight: "40px",
      padding: "10px 20px",
      borderRadius: "var(--radius-full)"
    }
  };
  const variants = {
    primary: {
      background: disabled ? "var(--color-primary-disabled)" : "var(--color-primary)",
      color: "var(--color-on-primary)"
    },
    secondary: {
      background: "var(--color-canvas)",
      color: "var(--color-ink)",
      borderColor: "var(--color-ink)"
    },
    tertiary: {
      background: "transparent",
      color: "var(--color-ink)",
      textDecoration: "underline",
      padding: "0",
      minHeight: "auto"
    },
    pill: {
      background: disabled ? "var(--color-primary-disabled)" : "var(--color-primary)",
      color: "var(--color-on-primary)"
    }
  };
  const sizeKey = variant === "pill" ? "pill" : size;
  return /*#__PURE__*/React.createElement("button", _extends({
    type: "button",
    disabled: disabled,
    style: {
      ...base,
      ...sizes[sizeKey],
      ...variants[variant],
      ...style
    }
  }, rest), children);
}
Object.assign(__ds_scope, { Button });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/Button.jsx", error: String((e && e.message) || e) }); }

// components/core/IconButton.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Mini-Commerce IconButton — a circular icon-only control.
 * `surface` = soft grey circle (toolbar / back-arrow). `ghost` = transparent.
 * `heart` = the wishlist save control: outlined glyph that fills Rausch when saved.
 */
function IconButton({
  variant = "surface",
  size = 40,
  saved = false,
  label = "button",
  children,
  style,
  ...rest
}) {
  const isHeart = variant === "heart";
  const base = {
    width: size,
    height: size,
    borderRadius: "var(--radius-full)",
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    cursor: "pointer",
    padding: 0,
    transition: "background 120ms ease, transform 120ms ease",
    flex: "none"
  };
  const variants = {
    surface: {
      background: "var(--color-surface-strong)",
      border: "none",
      color: "var(--color-ink)"
    },
    ghost: {
      background: "transparent",
      border: "1px solid var(--color-hairline)",
      color: "var(--color-ink)"
    },
    heart: {
      background: "transparent",
      border: "none",
      color: "var(--color-ink)"
    }
  };
  const heartGlyph = /*#__PURE__*/React.createElement("svg", {
    width: Math.round(size * 0.6),
    height: Math.round(size * 0.6),
    viewBox: "0 0 32 32",
    "aria-hidden": "true"
  }, /*#__PURE__*/React.createElement("path", {
    d: "M16 28S4 20.5 4 12.5C4 8.36 7.36 5 11.5 5c2.54 0 4.78 1.27 6.5 3.2C19.72 6.27 21.96 5 24.5 5 28.64 5 32 8.36 32 12.5 32 20.5 16 28 16 28Z",
    transform: "translate(-1 0)",
    fill: saved ? "var(--color-primary)" : "rgba(0,0,0,0.5)",
    stroke: saved ? "var(--color-primary)" : "#fff",
    strokeWidth: "2"
  }));
  return /*#__PURE__*/React.createElement("button", _extends({
    type: "button",
    "aria-label": label,
    "aria-pressed": isHeart ? saved : undefined,
    style: {
      ...base,
      ...variants[variant],
      ...style
    }
  }, rest), isHeart ? heartGlyph : children);
}
Object.assign(__ds_scope, { IconButton });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/IconButton.jsx", error: String((e && e.message) || e) }); }

// components/core/Input.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Mini-Commerce Input — white surface, 1px hairline, 8px radius, 56px tall,
 * stacked caption label. On focus the border thickens to 2px ink (no glow).
 */
function Input({
  label,
  id,
  error,
  value,
  defaultValue,
  placeholder,
  type = "text",
  style,
  ...rest
}) {
  const [focused, setFocused] = React.useState(false);
  const inputId = id || (label ? label.toLowerCase().replace(/\s+/g, "-") : undefined);
  const borderColor = error ? "var(--color-error)" : focused ? "var(--color-ink)" : "var(--color-hairline)";
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: "6px",
      ...style
    }
  }, label && /*#__PURE__*/React.createElement("label", {
    htmlFor: inputId,
    style: {
      fontFamily: "var(--font-sans)",
      fontSize: "var(--type-caption-size)",
      fontWeight: "var(--type-caption-weight)",
      color: "var(--color-muted)"
    }
  }, label), /*#__PURE__*/React.createElement("input", _extends({
    id: inputId,
    type: type,
    value: value,
    defaultValue: defaultValue,
    placeholder: placeholder,
    onFocus: () => setFocused(true),
    onBlur: () => setFocused(false),
    style: {
      fontFamily: "var(--font-sans)",
      fontSize: "var(--type-body-md-size)",
      color: "var(--color-ink)",
      background: "var(--color-canvas)",
      height: "56px",
      padding: "12px 14px",
      borderRadius: "var(--radius-sm)",
      border: `${focused || error ? "2px" : "1px"} solid ${borderColor}`,
      outline: "none",
      width: "100%",
      boxSizing: "border-box"
    }
  }, rest)), error && /*#__PURE__*/React.createElement("span", {
    style: {
      fontFamily: "var(--font-sans)",
      fontSize: "var(--type-caption-sm-size)",
      color: "var(--color-error)"
    }
  }, error));
}
Object.assign(__ds_scope, { Input });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/Input.jsx", error: String((e && e.message) || e) }); }

// components/core/StarRating.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Mini-Commerce StarRating — inline rating in INK (never gold; yellow stars
 * read cheap in a travel context). Single ink star glyph + numeric value,
 * optional review count.
 */
function StarRating({
  value,
  count,
  showCount = true,
  size = 14,
  style,
  ...rest
}) {
  const num = typeof value === "number" ? value.toFixed(2) : value;
  return /*#__PURE__*/React.createElement("span", _extends({
    style: {
      display: "inline-flex",
      alignItems: "center",
      gap: "4px",
      fontFamily: "var(--font-sans)",
      fontSize: size,
      fontWeight: "var(--weight-regular)",
      color: "var(--color-star-rating)",
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("svg", {
    width: size,
    height: size,
    viewBox: "0 0 16 16",
    "aria-hidden": "true",
    style: {
      marginTop: "-1px"
    }
  }, /*#__PURE__*/React.createElement("path", {
    d: "M8 1.2l1.9 4 4.4.6-3.2 3.1.8 4.4L8 11.3 4.1 13.3l.8-4.4L1.7 5.8l4.4-.6L8 1.2Z",
    fill: "var(--color-star-rating)"
  })), /*#__PURE__*/React.createElement("span", null, num), showCount && count != null && /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--color-muted)"
    }
  }, "\xB7 ", count, " reviews"));
}
Object.assign(__ds_scope, { StarRating });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/core/StarRating.jsx", error: String((e && e.message) || e) }); }

// components/marketplace/PropertyCard.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Mini-Commerce PropertyCard — the photo-first marketplace card.
 * 1:1 photo with 14px corner clipping, optional "Guest favorite" badge
 * top-left, wishlist heart top-right, then 4–5 lines of ink/muted meta.
 */
function PropertyCard({
  image,
  title,
  meta,
  dates,
  price,
  unit = "night",
  rating,
  favorite = false,
  saved: savedProp = false,
  style,
  ...rest
}) {
  const [saved, setSaved] = React.useState(savedProp);
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      fontFamily: "var(--font-sans)",
      width: "100%",
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "relative",
      aspectRatio: "1 / 1",
      borderRadius: "var(--radius-md)",
      overflow: "hidden",
      background: "var(--color-surface-strong)"
    }
  }, image && /*#__PURE__*/React.createElement("img", {
    src: image,
    alt: title,
    style: {
      width: "100%",
      height: "100%",
      objectFit: "cover",
      display: "block"
    }
  }), favorite && /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      top: "12px",
      left: "12px"
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Badge, {
    variant: "favorite"
  }, "Guest favorite")), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      top: "10px",
      right: "10px"
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.IconButton, {
    variant: "heart",
    size: 32,
    saved: saved,
    label: "Save",
    onClick: () => setSaved(s => !s)
  }))), /*#__PURE__*/React.createElement("div", {
    style: {
      paddingTop: "var(--space-md)",
      display: "flex",
      flexDirection: "column",
      gap: "2px"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "space-between",
      gap: "8px",
      alignItems: "baseline"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: "var(--type-title-md-size)",
      fontWeight: "var(--type-title-md-weight)",
      color: "var(--color-ink)"
    }
  }, title), rating != null && /*#__PURE__*/React.createElement(__ds_scope.StarRating, {
    value: rating,
    showCount: false
  })), meta && /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: "var(--type-body-sm-size)",
      color: "var(--color-muted)"
    }
  }, meta), dates && /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: "var(--type-body-sm-size)",
      color: "var(--color-muted)"
    }
  }, dates), price && /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: "var(--type-body-sm-size)",
      color: "var(--color-ink)",
      marginTop: "2px"
    }
  }, /*#__PURE__*/React.createElement("strong", {
    style: {
      fontWeight: "var(--weight-semibold)"
    }
  }, price), " ", unit)));
}
Object.assign(__ds_scope, { PropertyCard });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/marketplace/PropertyCard.jsx", error: String((e && e.message) || e) }); }

// components/marketplace/RatingDisplay.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Mini-Commerce RatingDisplay — the single loudest typographic moment.
 * A 64px / 700 rating number flanked by laurel-wreath ornaments, the
 * "Guest favorite" tagline, and a row of ink stat columns.
 */
function Laurel({
  flip = false
}) {
  return /*#__PURE__*/React.createElement("svg", {
    width: "34",
    height: "64",
    viewBox: "0 0 34 64",
    fill: "none",
    "aria-hidden": "true",
    style: {
      transform: flip ? "scaleX(-1)" : "none"
    }
  }, /*#__PURE__*/React.createElement("path", {
    d: "M28 6C18 11 13 21 14 32c1 11 6 21 14 26M24 12c-6 1-9 4-9 9M22 21c-6 0-9 3-9 8M21 31c-5 1-8 4-8 8M22 41c-5 1-7 4-7 8",
    stroke: "var(--color-ink)",
    strokeWidth: "1.6",
    strokeLinecap: "round",
    fill: "none"
  }));
}
function RatingDisplay({
  value = "4.94",
  tagline = "Guest favorite",
  caption = "One of the most loved homes on Mini-Commerce, according to guests",
  stats = [{
    value: "4.94",
    label: "Rating"
  }, {
    value: "Superhost",
    label: ""
  }, {
    value: "148",
    label: "Reviews"
  }],
  style,
  ...rest
}) {
  return /*#__PURE__*/React.createElement("section", _extends({
    style: {
      textAlign: "center",
      fontFamily: "var(--font-sans)",
      color: "var(--color-ink)",
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      gap: "var(--space-sm)"
    }
  }, /*#__PURE__*/React.createElement(Laurel, null), /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: "var(--type-rating-display-size)",
      fontWeight: "var(--type-rating-display-weight)",
      lineHeight: "var(--type-rating-display-leading)",
      letterSpacing: "var(--type-rating-display-tracking)"
    }
  }, value), /*#__PURE__*/React.createElement(Laurel, {
    flip: true
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--type-display-sm-size)",
      fontWeight: "var(--weight-semibold)",
      marginTop: "var(--space-sm)"
    }
  }, tagline), caption && /*#__PURE__*/React.createElement("p", {
    style: {
      fontSize: "var(--type-body-sm-size)",
      color: "var(--color-muted)",
      maxWidth: "320px",
      margin: "8px auto 0"
    }
  }, caption), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "inline-flex",
      marginTop: "var(--space-lg)",
      border: "1px solid var(--color-hairline)",
      borderRadius: "var(--radius-md)"
    }
  }, stats.map((s, i) => /*#__PURE__*/React.createElement("div", {
    key: i,
    style: {
      padding: "12px 24px",
      borderRight: i < stats.length - 1 ? "1px solid var(--color-hairline)" : "none"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--type-body-md-size)",
      fontWeight: "var(--weight-semibold)"
    }
  }, s.value), s.label && /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--type-caption-sm-size)",
      color: "var(--color-muted)"
    }
  }, s.label)))));
}
Object.assign(__ds_scope, { RatingDisplay });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/marketplace/RatingDisplay.jsx", error: String((e && e.message) || e) }); }

// components/marketplace/ReservationCard.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Mini-Commerce ReservationCard — sticky right-rail card on listing detail.
 * White surface, 14px rounding, hairline border, system shadow, 24px padding.
 * Nightly price, date-range selector, guest stepper, Reserve CTA, fee stack.
 */
function ReservationCard({
  price = "$182",
  unit = "night",
  rating,
  reviews,
  checkIn = "Add date",
  checkOut = "Add date",
  guests = "1 guest",
  fees = [{
    label: "$182 × 5 nights",
    value: "$910"
  }, {
    label: "Cleaning fee",
    value: "$45"
  }, {
    label: "Service fee",
    value: "$98"
  }],
  total = "$1,053",
  style,
  ...rest
}) {
  const cellLabel = {
    fontSize: "10px",
    fontWeight: 700,
    textTransform: "uppercase",
    letterSpacing: "0.4px",
    color: "var(--color-ink)"
  };
  const cellValue = {
    fontSize: "var(--type-body-sm-size)",
    color: "var(--color-muted)",
    marginTop: "2px"
  };
  return /*#__PURE__*/React.createElement("aside", _extends({
    style: {
      width: "372px",
      maxWidth: "100%",
      background: "var(--color-canvas)",
      border: "1px solid var(--color-hairline)",
      borderRadius: "var(--radius-md)",
      boxShadow: "var(--shadow-card)",
      padding: "var(--space-lg)",
      fontFamily: "var(--font-sans)",
      boxSizing: "border-box",
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "baseline",
      justifyContent: "space-between",
      marginBottom: "var(--space-base)"
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: "var(--type-display-md-size)",
      fontWeight: "var(--weight-semibold)",
      color: "var(--color-ink)"
    }
  }, price), /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: "var(--type-body-md-size)",
      color: "var(--color-body)"
    }
  }, " ", unit)), rating != null && /*#__PURE__*/React.createElement(__ds_scope.StarRating, {
    value: rating,
    count: reviews
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      border: "1px solid var(--color-hairline)",
      borderRadius: "12px",
      overflow: "hidden",
      marginBottom: "var(--space-base)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "1fr 1fr"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "10px 12px",
      borderRight: "1px solid var(--color-hairline)",
      borderBottom: "1px solid var(--color-hairline)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: cellLabel
  }, "Check-in"), /*#__PURE__*/React.createElement("div", {
    style: cellValue
  }, checkIn)), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "10px 12px",
      borderBottom: "1px solid var(--color-hairline)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: cellLabel
  }, "Checkout"), /*#__PURE__*/React.createElement("div", {
    style: cellValue
  }, checkOut))), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "10px 12px"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: cellLabel
  }, "Guests"), /*#__PURE__*/React.createElement("div", {
    style: cellValue
  }, guests))), /*#__PURE__*/React.createElement(__ds_scope.Button, {
    variant: "primary",
    fullWidth: true
  }, "Reserve"), /*#__PURE__*/React.createElement("p", {
    style: {
      textAlign: "center",
      fontSize: "var(--type-body-sm-size)",
      color: "var(--color-muted)",
      margin: "var(--space-base) 0"
    }
  }, "You won't be charged yet"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: "var(--space-md)"
    }
  }, fees.map(f => /*#__PURE__*/React.createElement("div", {
    key: f.label,
    style: {
      display: "flex",
      justifyContent: "space-between",
      fontSize: "var(--type-body-md-size)",
      color: "var(--color-body)"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      textDecoration: "underline"
    }
  }, f.label), /*#__PURE__*/React.createElement("span", null, f.value)))), /*#__PURE__*/React.createElement("div", {
    style: {
      borderTop: "1px solid var(--color-hairline)",
      marginTop: "var(--space-base)",
      paddingTop: "var(--space-base)",
      display: "flex",
      justifyContent: "space-between",
      fontSize: "var(--type-body-md-size)",
      fontWeight: "var(--weight-semibold)",
      color: "var(--color-ink)"
    }
  }, /*#__PURE__*/React.createElement("span", null, "Total"), /*#__PURE__*/React.createElement("span", null, total)));
}
Object.assign(__ds_scope, { ReservationCard });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/marketplace/ReservationCard.jsx", error: String((e && e.message) || e) }); }

// components/marketplace/SearchBar.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Mini-Commerce SearchBar — the signature global search pill.
 * White fill, fully rounded, the system shadow at rest, divided by 1px
 * hairlines into Where / When / Who segments, terminated by a circular
 * Rausch search orb.
 */
function SearchBar({
  segments,
  onSearch,
  style,
  ...rest
}) {
  const fields = segments || [{
    label: "Where",
    placeholder: "Search destinations"
  }, {
    label: "When",
    placeholder: "Add dates"
  }, {
    label: "Who",
    placeholder: "Add guests"
  }];
  return /*#__PURE__*/React.createElement("div", _extends({
    role: "search",
    style: {
      display: "inline-flex",
      alignItems: "center",
      background: "var(--color-canvas)",
      borderRadius: "var(--radius-full)",
      boxShadow: "var(--shadow-card)",
      height: "64px",
      padding: "0 8px 0 0",
      fontFamily: "var(--font-sans)",
      ...style
    }
  }, rest), fields.map((f, i) => /*#__PURE__*/React.createElement(React.Fragment, {
    key: f.label
  }, i > 0 && /*#__PURE__*/React.createElement("span", {
    style: {
      width: "1px",
      height: "32px",
      background: "var(--color-hairline)"
    }
  }), /*#__PURE__*/React.createElement("button", {
    type: "button",
    style: {
      display: "flex",
      flexDirection: "column",
      gap: "2px",
      alignItems: "flex-start",
      background: "transparent",
      border: "none",
      cursor: "pointer",
      padding: "14px 20px",
      borderRadius: "var(--radius-full)",
      textAlign: "left"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: "var(--type-caption-size)",
      fontWeight: "var(--type-caption-weight)",
      color: "var(--color-ink)"
    }
  }, f.label), /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: "var(--type-body-sm-size)",
      color: "var(--color-muted)"
    }
  }, f.placeholder)))), /*#__PURE__*/React.createElement("button", {
    type: "button",
    "aria-label": "Search",
    onClick: onSearch,
    style: {
      width: "48px",
      height: "48px",
      borderRadius: "var(--radius-full)",
      background: "var(--color-primary)",
      border: "none",
      cursor: "pointer",
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      marginLeft: "8px",
      flex: "none"
    }
  }, /*#__PURE__*/React.createElement("svg", {
    width: "18",
    height: "18",
    viewBox: "0 0 24 24",
    fill: "none",
    "aria-hidden": "true"
  }, /*#__PURE__*/React.createElement("circle", {
    cx: "11",
    cy: "11",
    r: "7",
    stroke: "#fff",
    strokeWidth: "2.4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "m20 20-3.5-3.5",
    stroke: "#fff",
    strokeWidth: "2.4",
    strokeLinecap: "round"
  }))));
}
Object.assign(__ds_scope, { SearchBar });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/marketplace/SearchBar.jsx", error: String((e && e.message) || e) }); }

// components/marketplace/TopNav.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
const ICONS = {
  Homes: /*#__PURE__*/React.createElement("svg", {
    width: "28",
    height: "28",
    viewBox: "0 0 32 32",
    fill: "none",
    "aria-hidden": "true"
  }, /*#__PURE__*/React.createElement("path", {
    d: "M5 14.5 16 5l11 9.5",
    stroke: "currentColor",
    strokeWidth: "2",
    strokeLinecap: "round",
    strokeLinejoin: "round"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M7.5 13v12.5a1.5 1.5 0 0 0 1.5 1.5h14a1.5 1.5 0 0 0 1.5-1.5V13",
    stroke: "currentColor",
    strokeWidth: "2",
    strokeLinecap: "round",
    strokeLinejoin: "round"
  }), /*#__PURE__*/React.createElement("rect", {
    x: "13",
    y: "19",
    width: "6",
    height: "8",
    rx: "1",
    stroke: "currentColor",
    strokeWidth: "2"
  })),
  Experiences: /*#__PURE__*/React.createElement("svg", {
    width: "28",
    height: "28",
    viewBox: "0 0 32 32",
    fill: "none",
    "aria-hidden": "true"
  }, /*#__PURE__*/React.createElement("path", {
    d: "M16 3c-5.2 0-9.5 4-9.5 9 0 4.1 3.1 7.4 6.4 10.2l.6 2.3a1.5 1.5 0 0 0 1.45 1.1h2.1a1.5 1.5 0 0 0 1.45-1.1l.6-2.3C22.4 19.4 25.5 16.1 25.5 12c0-5-4.3-9-9.5-9Z",
    stroke: "currentColor",
    strokeWidth: "2",
    strokeLinejoin: "round"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M16 3v19M11 5.5c1.6 5 1.6 11.5 0 16.5M21 5.5c-1.6 5-1.6 11.5 0 16.5",
    stroke: "currentColor",
    strokeWidth: "1.4",
    strokeLinecap: "round"
  })),
  Services: /*#__PURE__*/React.createElement("svg", {
    width: "28",
    height: "28",
    viewBox: "0 0 32 32",
    fill: "none",
    "aria-hidden": "true"
  }, /*#__PURE__*/React.createElement("path", {
    d: "M6 23c0-5.52 4.48-10 10-10s10 4.48 10 10",
    stroke: "currentColor",
    strokeWidth: "2",
    strokeLinejoin: "round"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M4 23h24M16 7v3",
    stroke: "currentColor",
    strokeWidth: "2",
    strokeLinecap: "round"
  }), /*#__PURE__*/React.createElement("circle", {
    cx: "16",
    cy: "6",
    r: "1.6",
    fill: "currentColor"
  }))
};
function ProductTab({
  label,
  active,
  isNew,
  onClick
}) {
  return /*#__PURE__*/React.createElement("button", {
    type: "button",
    onClick: onClick,
    style: {
      display: "flex",
      flexDirection: "column",
      alignItems: "center",
      gap: "2px",
      background: "transparent",
      border: "none",
      cursor: "pointer",
      padding: "6px 4px 4px",
      position: "relative",
      color: active ? "var(--color-ink)" : "var(--color-muted)",
      fontFamily: "var(--font-sans)"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      position: "relative",
      display: "inline-flex"
    }
  }, ICONS[label], isNew && /*#__PURE__*/React.createElement("span", {
    style: {
      position: "absolute",
      top: "-8px",
      right: "-18px"
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Badge, {
    variant: "new"
  }))), /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: "var(--type-nav-link-size)",
      fontWeight: "var(--type-nav-link-weight)"
    }
  }, label), /*#__PURE__*/React.createElement("span", {
    style: {
      position: "absolute",
      bottom: "-1px",
      left: "4px",
      right: "4px",
      height: "2px",
      borderRadius: "2px",
      background: active ? "var(--color-ink)" : "transparent"
    }
  }));
}

/**
 * Mini-Commerce TopNav — white 80px bar, bottom hairline. Wordmark flush left,
 * three product tabs (Homes / Experiences / Services) dead center, account
 * utilities flush right.
 */
function TopNav({
  active = "Homes",
  logoSrc,
  onSelect,
  style,
  ...rest
}) {
  const tabs = [{
    label: "Homes",
    isNew: false
  }, {
    label: "Experiences",
    isNew: true
  }, {
    label: "Services",
    isNew: true
  }];
  return /*#__PURE__*/React.createElement("header", _extends({
    style: {
      display: "flex",
      alignItems: "center",
      justifyContent: "space-between",
      height: "80px",
      padding: "0 var(--space-xxl)",
      background: "var(--color-canvas)",
      borderBottom: "1px solid var(--color-hairline)",
      fontFamily: "var(--font-sans)",
      ...style
    }
  }, rest), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: "1 1 0",
      display: "flex",
      alignItems: "center"
    }
  }, logoSrc ? /*#__PURE__*/React.createElement("img", {
    src: logoSrc,
    alt: "Mini-Commerce",
    style: {
      height: "32px"
    }
  }) : /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: "20px",
      fontWeight: 700,
      color: "var(--color-primary)",
      letterSpacing: "-0.4px"
    }
  }, "minicommerce")), /*#__PURE__*/React.createElement("nav", {
    style: {
      display: "flex",
      gap: "var(--space-lg)",
      alignItems: "flex-end",
      height: "100%"
    }
  }, tabs.map(t => /*#__PURE__*/React.createElement(ProductTab, _extends({
    key: t.label
  }, t, {
    active: active === t.label,
    onClick: () => onSelect && onSelect(t.label)
  })))), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: "1 1 0",
      display: "flex",
      alignItems: "center",
      justifyContent: "flex-end",
      gap: "var(--space-sm)"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: "var(--type-body-sm-size)",
      fontWeight: "var(--weight-semibold)",
      color: "var(--color-ink)",
      padding: "12px",
      cursor: "pointer"
    }
  }, "Become a host"), /*#__PURE__*/React.createElement("span", {
    style: {
      width: "40px",
      height: "40px",
      borderRadius: "var(--radius-full)",
      background: "var(--color-surface-strong)",
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center"
    },
    "aria-label": "Language"
  }, /*#__PURE__*/React.createElement("svg", {
    width: "18",
    height: "18",
    viewBox: "0 0 24 24",
    fill: "none",
    "aria-hidden": "true"
  }, /*#__PURE__*/React.createElement("circle", {
    cx: "12",
    cy: "12",
    r: "9",
    stroke: "var(--color-ink)",
    strokeWidth: "1.6"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M3 12h18M12 3c2.5 2.5 2.5 15 0 18M12 3c-2.5 2.5-2.5 15 0 18",
    stroke: "var(--color-ink)",
    strokeWidth: "1.4"
  }))), /*#__PURE__*/React.createElement("span", {
    style: {
      display: "inline-flex",
      alignItems: "center",
      gap: "8px",
      border: "1px solid var(--color-hairline)",
      borderRadius: "var(--radius-full)",
      padding: "5px 6px 5px 12px",
      cursor: "pointer"
    }
  }, /*#__PURE__*/React.createElement("svg", {
    width: "16",
    height: "16",
    viewBox: "0 0 24 24",
    fill: "none",
    "aria-hidden": "true"
  }, /*#__PURE__*/React.createElement("path", {
    d: "M4 7h16M4 12h16M4 17h16",
    stroke: "var(--color-ink)",
    strokeWidth: "1.8",
    strokeLinecap: "round"
  })), /*#__PURE__*/React.createElement("span", {
    style: {
      width: "30px",
      height: "30px",
      borderRadius: "var(--radius-full)",
      background: "var(--color-muted)",
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      color: "#fff",
      fontSize: "12px",
      fontWeight: 600
    }
  }, "A"))));
}
Object.assign(__ds_scope, { TopNav });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/marketplace/TopNav.jsx", error: String((e && e.message) || e) }); }

// ui_kits/marketplace/Home.jsx
try { (() => {
// Mini-Commerce — Home screen. Composes TopNav, SearchBar, category strip,
// a 4-up property grid, and the city-link footer grid.
function Home({
  ns,
  onOpenListing,
  onSearch,
  onSelectTab,
  activeTab
}) {
  const {
    TopNav,
    SearchBar,
    PropertyCard,
    Button
  } = ns;
  const {
    listings,
    categories,
    cities
  } = window.MC_DATA;
  const [cat, setCat] = React.useState("Amazing views");
  return /*#__PURE__*/React.createElement("div", {
    style: {
      background: "var(--color-canvas)",
      minHeight: "100%",
      fontFamily: "var(--font-sans)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      position: "sticky",
      top: 0,
      zIndex: 10,
      background: "var(--color-canvas)"
    }
  }, /*#__PURE__*/React.createElement(TopNav, {
    active: activeTab,
    logoSrc: "../../assets/logo-wordmark.svg",
    onSelect: onSelectTab
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "center",
      padding: "20px 0 16px",
      borderBottom: "1px solid var(--color-hairline)"
    }
  }, /*#__PURE__*/React.createElement(SearchBar, {
    onSearch: onSearch
  }))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: "var(--space-xl)",
      overflowX: "auto",
      padding: "16px var(--space-xxl)",
      maxWidth: "var(--container-editorial)",
      margin: "0 auto"
    }
  }, categories.map(c => /*#__PURE__*/React.createElement("button", {
    key: c,
    onClick: () => setCat(c),
    style: {
      background: "transparent",
      border: "none",
      cursor: "pointer",
      padding: "8px 0",
      borderBottom: cat === c ? "2px solid var(--color-ink)" : "2px solid transparent",
      color: cat === c ? "var(--color-ink)" : "var(--color-muted)",
      fontSize: "var(--type-button-sm-size)",
      fontWeight: 600,
      whiteSpace: "nowrap",
      opacity: cat === c ? 1 : 0.8
    }
  }, c))), /*#__PURE__*/React.createElement("div", {
    style: {
      maxWidth: "var(--container-editorial)",
      margin: "0 auto",
      padding: "var(--space-lg) var(--space-xxl) var(--space-section)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(4, 1fr)",
      gap: "var(--space-base) var(--space-lg)"
    }
  }, listings.map(l => /*#__PURE__*/React.createElement("div", {
    key: l.id,
    style: {
      cursor: "pointer"
    },
    onClick: () => onOpenListing(l.id)
  }, /*#__PURE__*/React.createElement(PropertyCard, l))))), /*#__PURE__*/React.createElement("div", {
    style: {
      background: "var(--color-surface-soft)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      maxWidth: "var(--container-editorial)",
      margin: "0 auto",
      padding: "var(--space-section) var(--space-xxl)"
    }
  }, /*#__PURE__*/React.createElement("h2", {
    style: {
      fontSize: "var(--type-display-md-size)",
      fontWeight: 700,
      color: "var(--color-ink)",
      margin: "0 0 var(--space-lg)"
    }
  }, "Inspiration for future getaways"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(6, 1fr)",
      gap: "var(--space-base)"
    }
  }, cities.map(c => /*#__PURE__*/React.createElement("div", {
    key: c.name,
    style: {
      paddingTop: "12px",
      borderTop: "1px solid var(--color-hairline)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--type-title-md-size)",
      fontWeight: 600,
      color: "var(--color-ink)"
    }
  }, c.name), /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--type-body-sm-size)",
      color: "var(--color-muted)"
    }
  }, c.sub)))), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: "var(--space-xl)"
    }
  }, /*#__PURE__*/React.createElement(Button, {
    variant: "pill"
  }, "Become a host")))));
}
window.Home = Home;
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/marketplace/Home.jsx", error: String((e && e.message) || e) }); }

// ui_kits/marketplace/ListingDetail.jsx
try { (() => {
// Mini-Commerce — Listing detail. Photo banner, h1, 2-column body with a
// sticky reservation rail, the Guest-favorite rating banner, amenities, host.
function ListingDetail({
  ns,
  listingId,
  onSelectTab,
  activeTab,
  onBack
}) {
  const {
    TopNav,
    ReservationCard,
    RatingDisplay,
    Avatar,
    Button,
    IconButton
  } = ns;
  const {
    listings,
    amenities
  } = window.MC_DATA;
  const l = listings.find(x => x.id === listingId) || listings[0];
  const photos = [l.image, "https://images.unsplash.com/photo-1600566753086-00f18fb6b3ea?w=600&q=80", "https://images.unsplash.com/photo-1505691938895-1758d7feb511?w=600&q=80", "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?w=600&q=80", "https://images.unsplash.com/photo-1556909211-36987daf7b4d?w=600&q=80"];
  return /*#__PURE__*/React.createElement("div", {
    style: {
      background: "var(--color-canvas)",
      minHeight: "100%",
      fontFamily: "var(--font-sans)"
    }
  }, /*#__PURE__*/React.createElement(TopNav, {
    active: activeTab,
    logoSrc: "../../assets/logo-wordmark.svg",
    onSelect: onSelectTab
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      maxWidth: "var(--container-listing)",
      margin: "0 auto",
      padding: "var(--space-lg) var(--space-xxl)"
    }
  }, /*#__PURE__*/React.createElement("button", {
    onClick: onBack,
    style: {
      background: "none",
      border: "none",
      cursor: "pointer",
      fontSize: 14,
      color: "var(--color-ink)",
      textDecoration: "underline",
      padding: 0,
      marginBottom: 12
    }
  }, "\u2190 All homes"), /*#__PURE__*/React.createElement("h1", {
    style: {
      fontSize: "var(--type-display-lg-size)",
      fontWeight: "var(--type-display-lg-weight)",
      letterSpacing: "var(--type-display-lg-tracking)",
      color: "var(--color-ink)",
      margin: "0 0 12px"
    }
  }, l.meta, " in ", l.title), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "2fr 1fr 1fr",
      gridTemplateRows: "1fr 1fr",
      gap: "8px",
      height: "420px",
      borderRadius: "var(--radius-md)",
      overflow: "hidden",
      marginBottom: "var(--space-section)"
    }
  }, photos.slice(0, 5).map((p, i) => /*#__PURE__*/React.createElement("img", {
    key: i,
    src: p,
    alt: "",
    style: {
      width: "100%",
      height: "100%",
      objectFit: "cover",
      gridRow: i === 0 ? "1 / 3" : "auto",
      gridColumn: i === 0 ? "1" : "auto",
      display: "block",
      background: "var(--color-surface-strong)"
    }
  }))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "1fr 372px",
      gap: "var(--space-section)",
      alignItems: "start"
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "space-between",
      alignItems: "flex-start",
      paddingBottom: "var(--space-lg)",
      borderBottom: "1px solid var(--color-hairline)"
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--type-display-sm-size)",
      fontWeight: 600,
      color: "var(--color-ink)"
    }
  }, "Entire home hosted by Aylin"), /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--type-body-md-size)",
      color: "var(--color-body)",
      marginTop: 4
    }
  }, "6 guests \xB7 3 bedrooms \xB7 4 beds \xB7 2 baths")), /*#__PURE__*/React.createElement(Avatar, {
    name: "Aylin K",
    size: 56,
    superhost: true
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "var(--space-section) 0",
      borderBottom: "1px solid var(--color-hairline)"
    }
  }, /*#__PURE__*/React.createElement(RatingDisplay, {
    value: String(l.rating),
    stats: [{
      value: String(l.rating),
      label: "Rating"
    }, {
      value: "Superhost"
    }, {
      value: "148",
      label: "Reviews"
    }]
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "var(--space-section) 0",
      borderBottom: "1px solid var(--color-hairline)"
    }
  }, /*#__PURE__*/React.createElement("h2", {
    style: {
      fontSize: "var(--type-display-md-size)",
      fontWeight: 700,
      color: "var(--color-ink)",
      margin: "0 0 var(--space-lg)"
    }
  }, "What this place offers"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "1fr 1fr",
      gap: "var(--space-base) var(--space-xl)"
    }
  }, amenities.map(a => /*#__PURE__*/React.createElement("div", {
    key: a,
    style: {
      display: "flex",
      alignItems: "center",
      gap: "var(--space-base)",
      padding: "12px 0",
      fontSize: "var(--type-body-md-size)",
      color: "var(--color-body)"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      width: 22,
      height: 22,
      borderRadius: 6,
      background: "var(--color-surface-strong)",
      flex: "none"
    }
  }), a))), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: "var(--space-base)"
    }
  }, /*#__PURE__*/React.createElement(Button, {
    variant: "secondary"
  }, "Show all 32 amenities"))), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "var(--space-section) 0 0"
    }
  }, /*#__PURE__*/React.createElement("h2", {
    style: {
      fontSize: "var(--type-display-md-size)",
      fontWeight: 700,
      color: "var(--color-ink)",
      margin: "0 0 var(--space-lg)"
    }
  }, "Meet your host"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: "var(--space-lg)",
      border: "1px solid var(--color-hairline)",
      borderRadius: "var(--radius-md)",
      boxShadow: "var(--shadow-card)",
      padding: "var(--space-lg)",
      alignItems: "center"
    }
  }, /*#__PURE__*/React.createElement(Avatar, {
    name: "Aylin K",
    size: 72,
    superhost: true
  }), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--type-display-md-size)",
      fontWeight: 700,
      color: "var(--color-ink)"
    }
  }, "Aylin"), /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--type-body-sm-size)",
      color: "var(--color-muted)"
    }
  }, "Superhost \xB7 312 reviews \xB7 98% response rate"), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 12
    }
  }, /*#__PURE__*/React.createElement(Button, {
    variant: "secondary",
    size: "sm"
  }, "Contact host")))))), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "sticky",
      top: "100px"
    }
  }, /*#__PURE__*/React.createElement(ReservationCard, {
    price: l.price,
    rating: l.rating,
    reviews: 148,
    checkIn: l.dates.split(" – ")[0],
    checkOut: l.dates.split(" – ")[1],
    guests: "2 guests",
    total: "$1,053"
  })))));
}
window.ListingDetail = ListingDetail;
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/marketplace/ListingDetail.jsx", error: String((e && e.message) || e) }); }

// ui_kits/marketplace/SearchResults.jsx
try { (() => {
// Mini-Commerce — Search results. TopNav + filter band + 3-up result grid.
function SearchResults({
  ns,
  onOpenListing,
  onSelectTab,
  activeTab,
  onHome
}) {
  const {
    TopNav,
    PropertyCard,
    Button
  } = ns;
  const {
    listings
  } = window.MC_DATA;
  const filters = ["Type of place", "Price", "Rooms", "Instant Book", "More filters"];
  return /*#__PURE__*/React.createElement("div", {
    style: {
      background: "var(--color-canvas)",
      minHeight: "100%",
      fontFamily: "var(--font-sans)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      position: "sticky",
      top: 0,
      zIndex: 10,
      background: "var(--color-canvas)"
    }
  }, /*#__PURE__*/React.createElement(TopNav, {
    active: activeTab,
    logoSrc: "../../assets/logo-wordmark.svg",
    onSelect: onSelectTab
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: "var(--space-md)",
      padding: "14px var(--space-xxl)",
      borderBottom: "1px solid var(--color-hairline)",
      overflowX: "auto"
    }
  }, /*#__PURE__*/React.createElement("button", {
    onClick: onHome,
    style: {
      display: "inline-flex",
      alignItems: "center",
      gap: "var(--space-base)",
      border: "1px solid var(--color-hairline)",
      borderRadius: "var(--radius-full)",
      boxShadow: "var(--shadow-card)",
      padding: "8px 8px 8px 18px",
      cursor: "pointer",
      background: "#fff",
      whiteSpace: "nowrap"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: 14,
      fontWeight: 600,
      color: "var(--color-ink)"
    }
  }, "Anywhere"), /*#__PURE__*/React.createElement("span", {
    style: {
      width: 1,
      height: 22,
      background: "var(--color-hairline)"
    }
  }), /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: 14,
      fontWeight: 600,
      color: "var(--color-ink)"
    }
  }, "Any week"), /*#__PURE__*/React.createElement("span", {
    style: {
      width: 1,
      height: 22,
      background: "var(--color-hairline)"
    }
  }), /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: 14,
      color: "var(--color-muted)"
    }
  }, "Add guests"), /*#__PURE__*/React.createElement("span", {
    style: {
      width: 34,
      height: 34,
      borderRadius: "var(--radius-full)",
      background: "var(--color-primary)",
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      marginLeft: 4
    }
  }, /*#__PURE__*/React.createElement("svg", {
    width: "14",
    height: "14",
    viewBox: "0 0 24 24",
    fill: "none"
  }, /*#__PURE__*/React.createElement("circle", {
    cx: "11",
    cy: "11",
    r: "7",
    stroke: "#fff",
    strokeWidth: "2.6"
  }), /*#__PURE__*/React.createElement("path", {
    d: "m20 20-3.5-3.5",
    stroke: "#fff",
    strokeWidth: "2.6",
    strokeLinecap: "round"
  })))), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }), filters.map(f => /*#__PURE__*/React.createElement("button", {
    key: f,
    style: {
      border: "1px solid var(--color-hairline)",
      borderRadius: "var(--radius-sm)",
      padding: "10px 14px",
      background: "#fff",
      cursor: "pointer",
      fontSize: 14,
      color: "var(--color-ink)",
      whiteSpace: "nowrap"
    }
  }, f)))), /*#__PURE__*/React.createElement("div", {
    style: {
      maxWidth: "var(--container-wide)",
      margin: "0 auto",
      padding: "var(--space-lg) var(--space-xxl) var(--space-section)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--type-body-sm-size)",
      color: "var(--color-muted)",
      marginBottom: "var(--space-base)"
    }
  }, "Over 1,000 places \xB7 T\xFCrkiye and nearby"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(3, 1fr)",
      gap: "var(--space-lg)"
    }
  }, [...listings, ...listings.slice(0, 4)].map((l, i) => /*#__PURE__*/React.createElement("div", {
    key: i,
    style: {
      cursor: "pointer"
    },
    onClick: () => onOpenListing(l.id)
  }, /*#__PURE__*/React.createElement(PropertyCard, l)))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "center",
      marginTop: "var(--space-xl)"
    }
  }, /*#__PURE__*/React.createElement(Button, {
    variant: "secondary"
  }, "Show more"))));
}
window.SearchResults = SearchResults;
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/marketplace/SearchResults.jsx", error: String((e && e.message) || e) }); }

// ui_kits/marketplace/data.js
try { (() => {
// Shared mock data for the Mini-Commerce marketplace UI kit.
window.MC_DATA = function () {
  const listings = [{
    id: 1,
    image: "https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?w=800&q=80",
    title: "Çeşme, Türkiye",
    meta: "Sea view villa",
    dates: "Nov 3 – 8",
    price: "$182",
    rating: 4.94,
    favorite: true
  }, {
    id: 2,
    image: "https://images.unsplash.com/photo-1493809842364-78817add7ffb?w=800&q=80",
    title: "Lake Tahoe, US",
    meta: "A-frame cabin",
    dates: "Dec 1 – 6",
    price: "$240",
    rating: 4.88,
    favorite: false
  }, {
    id: 3,
    image: "https://images.unsplash.com/photo-1567767292278-a4f21aa2d36e?w=800&q=80",
    title: "Santorini, Greece",
    meta: "Cliffside suite",
    dates: "Oct 12 – 17",
    price: "$310",
    rating: 4.97,
    favorite: true
  }, {
    id: 4,
    image: "https://images.unsplash.com/photo-1568605114967-8130f3a36994?w=800&q=80",
    title: "Big Sur, US",
    meta: "Glass cabin",
    dates: "Nov 20 – 25",
    price: "$420",
    rating: 4.91,
    favorite: false
  }, {
    id: 5,
    image: "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?w=800&q=80",
    title: "Cotswolds, UK",
    meta: "Country cottage",
    dates: "Jan 8 – 12",
    price: "$160",
    rating: 4.85,
    favorite: false
  }, {
    id: 6,
    image: "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=800&q=80",
    title: "Lisbon, Portugal",
    meta: "Tiled townhouse",
    dates: "Sep 5 – 10",
    price: "$135",
    rating: 4.79,
    favorite: false
  }, {
    id: 7,
    image: "https://images.unsplash.com/photo-1502005229762-cf1b2da7c5d6?w=800&q=80",
    title: "Kyoto, Japan",
    meta: "Machiya stay",
    dates: "Apr 2 – 7",
    price: "$198",
    rating: 4.96,
    favorite: true
  }, {
    id: 8,
    image: "https://images.unsplash.com/photo-1560448204-e02f11c3d0e2?w=800&q=80",
    title: "Tulum, Mexico",
    meta: "Jungle loft",
    dates: "Feb 14 – 19",
    price: "$176",
    rating: 4.83,
    favorite: false
  }];
  const categories = ["Amazing views", "Beachfront", "Cabins", "Tiny homes", "Design", "Treehouses", "Islands", "Lakefront", "A-frames", "Trending", "Castles", "OMG!"];
  const cities = [{
    name: "Wilmington",
    sub: "Cottage rentals"
  }, {
    name: "Athens",
    sub: "Villa rentals"
  }, {
    name: "Lisbon",
    sub: "Apartment rentals"
  }, {
    name: "Çeşme",
    sub: "Beach house rentals"
  }, {
    name: "Kyoto",
    sub: "Machiya rentals"
  }, {
    name: "Tulum",
    sub: "Loft rentals"
  }];
  const amenities = ["Sea view", "Kitchen", "Wifi", "Free parking", "Private pool", "Air conditioning", "Washer", "Dedicated workspace"];
  return {
    listings,
    categories,
    cities,
    amenities
  };
}();
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/marketplace/data.js", error: String((e && e.message) || e) }); }

__ds_ns.Avatar = __ds_scope.Avatar;

__ds_ns.Badge = __ds_scope.Badge;

__ds_ns.Button = __ds_scope.Button;

__ds_ns.IconButton = __ds_scope.IconButton;

__ds_ns.Input = __ds_scope.Input;

__ds_ns.StarRating = __ds_scope.StarRating;

__ds_ns.PropertyCard = __ds_scope.PropertyCard;

__ds_ns.RatingDisplay = __ds_scope.RatingDisplay;

__ds_ns.ReservationCard = __ds_scope.ReservationCard;

__ds_ns.SearchBar = __ds_scope.SearchBar;

__ds_ns.TopNav = __ds_scope.TopNav;

})();
