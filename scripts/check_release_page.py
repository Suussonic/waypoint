#!/usr/bin/env python3
"""Check the public release page's structure, accessibility hooks and local links."""

from html.parser import HTMLParser
from pathlib import Path
import re
from urllib.parse import urlsplit


class PageParser(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.tags = []
        self.ids = []
        self.headings = []
        self.images = []
        self.links = []
        self.metas = []
        self.title_text = ""
        self.in_title = False

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        self.tags.append((tag, attrs))
        if "id" in attrs:
            self.ids.append(attrs["id"])
        if tag in {"h1", "h2", "h3"}:
            self.headings.append((tag, attrs.get("id", "")))
        if tag == "img":
            self.images.append(attrs.get("src", ""))
        if tag in {"a", "link"}:
            self.links.append(attrs.get("href", ""))
        if tag == "meta":
            self.metas.append(attrs)
        if tag == "title":
            self.in_title = True

    def handle_endtag(self, tag):
        if tag == "title":
            self.in_title = False

    def handle_data(self, data):
        if self.in_title:
            self.title_text += data.strip()


def validate_document(path: Path, docs_root: Path) -> list[str]:
    errors = []
    if not path.is_file():
        return [f"Missing release page: {path.relative_to(docs_root)}"]
    source = path.read_text(encoding="utf-8")
    parser = PageParser()
    parser.feed(source)
    attributes = {(tag, key, value) for tag, attrs in parser.tags for key, value in attrs.items()}

    if not re.search(r"<html\b[^>]*\blang=['\"]en['\"]", source, re.IGNORECASE):
        errors.append(f"{path.name} must declare English as its document language")
    if not parser.title_text.strip():
        errors.append(f"{path.name} needs a page title")
    if not any(tag == "meta" and attrs.get("name", "").lower() == "viewport" for tag, attrs in parser.tags):
        errors.append(f"{path.name} needs a viewport meta tag")
    if parser.ids.count("main-content") != 1 or ("a", "href", "#main-content") not in attributes:
        errors.append(f"{path.name} needs a skip link to one main-content landmark")
    if len(parser.ids) != len(set(parser.ids)):
        errors.append(f"{path.name} has duplicate id attributes")

    previous_level = 0
    for tag, _ in parser.headings:
        level = int(tag[1])
        if level > previous_level + 1:
            errors.append(f"{path.name} skips a heading level before {tag}")
            break
        previous_level = level
    if parser.headings and parser.headings[0][0] != "h1":
        errors.append(f"{path.name} must start its heading outline with h1")

    for reference in parser.images + parser.links:
        if not reference or reference.startswith("#"):
            continue
        parsed = urlsplit(reference)
        if parsed.scheme or parsed.netloc or reference.startswith("//"):
            continue
        target = (path.parent / parsed.path).resolve()
        try:
            target.relative_to(docs_root.resolve())
        except ValueError:
            errors.append(f"{path.name} links outside docs/: {reference}")
            continue
        if parsed.path and not target.exists():
            errors.append(f"{path.name} has a missing local target: {reference}")

    return errors


def validate_release_site(docs_root: Path, version: str | None = None) -> list[str]:
    if version is None:
        version_file = docs_root.parent / "version.properties"
        values = dict(line.split("=", 1) for line in version_file.read_text(encoding="utf-8").splitlines() if "=" in line)
        version = values["VERSION_NAME"]
    errors = []
    versioned = docs_root / "releases" / f"{version}.html"
    current = docs_root / "releases" / "current.html"
    for page in (versioned, current):
        errors.extend(validate_document(page, docs_root))
    if versioned.is_file():
        source = versioned.read_text(encoding="utf-8")
        for required in ("Smoother motion on car displays", "Zoom where you choose", "Credits for the fixes"):
            if required not in source:
                errors.append(f"{versioned.name} is missing the '{required}' release section")
    if current.is_file():
        source = current.read_text(encoding="utf-8")
        if not re.search(rf"url=['\"]?{re.escape(version)}\.html", source, re.IGNORECASE):
            errors.append(f"current.html must redirect to {version}.html")
        if not re.search(rf"href=['\"]{re.escape(version)}\.html['\"]", source, re.IGNORECASE):
            errors.append("current.html must include an accessible link to the versioned release page")
    return errors


def main() -> None:
    root = Path(__file__).resolve().parents[1] / "docs"
    errors = validate_release_site(root)
    if errors:
        raise SystemExit("\n".join(f"- {error}" for error in errors))
    version = dict(line.split("=", 1) for line in (root.parent / "version.properties").read_text(encoding="utf-8").splitlines() if "=" in line)["VERSION_NAME"]
    print(f"Waypoint {version} release page and local assets verified")


if __name__ == "__main__":
    main()
