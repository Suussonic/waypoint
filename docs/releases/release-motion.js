(() => {
  const page = document.querySelector(".release-v266");
  const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)");
  if (!page || reduceMotion.matches || !("IntersectionObserver" in window) || !Element.prototype.animate) return;

  const groups = [
    [".release-nav nav a", "text"],
    [".release-hero h1", "title"],
    [".release-lede, .hero-actions a, .hero-stamp", "text"],
    [".map-road, .map-route-halo", "road"],
    [".map-node", "marker"],
    [".release-section .section-heading h2, .release-section .section-heading > p", "text"],
    [".feature-card h3, .feature-card p", "text"],
    [".bearing-ring", "ring"],
    [".bearing-arrow", "bearing"],
    [".route-note-line", "line"],
    [".route-note p, .credit-group h3, .credit-list li", "text"],
    [".release-footer-cta h2, .release-footer-cta p, .release-footer-cta a, .release-footer > a", "text"],
  ];
  const targets = groups.flatMap(([selector, kind]) =>
    [...page.querySelectorAll(selector)].map((element) => ({ element, kind })),
  );

  const observer = new IntersectionObserver((entries) => {
    for (const entry of entries) {
      if (!entry.isIntersecting) continue;
      const { element, kind } = entry.target.motion;
      const siblings = [...element.parentElement.children];
      const delay = Math.min(siblings.indexOf(element) * 65, 195);
      const options = {
        duration: kind === "title" ? 680 : kind === "road" ? 760 : 480,
        delay,
        easing: "cubic-bezier(.16, 1, .3, 1)",
        fill: "both",
      };
      let frames;

      if (kind === "line") {
        frames = [{ opacity: 0.35, transform: "scaleX(.12)" }, { opacity: 1, transform: "scaleX(1)" }];
      } else if (kind === "road") {
        frames = [{ opacity: 0.12 }, { opacity: 1 }];
      } else if (kind === "marker") {
        frames = [{ opacity: 0.35, transform: "scale(.55)" }, { opacity: 1, transform: "scale(1)" }];
      } else if (kind === "ring") {
        frames = [
          { opacity: 0.35, transform: "translate(-50%, -50%) scale(.92)" },
          { opacity: 1, transform: "translate(-50%, -50%) scale(1)" },
        ];
      } else if (kind === "bearing") {
        frames = [{ transform: "rotate(-14deg)" }, { transform: "rotate(0deg)" }];
      } else if (kind === "title") {
        frames = [
          { opacity: 0.55, transform: "translate3d(0, 1rem, 0)", clipPath: "inset(0 0 24% 0)" },
          { opacity: 1, transform: "translate3d(0, 0, 0)", clipPath: "inset(0)" },
        ];
      } else {
        frames = [{ opacity: 0.68, transform: "translate3d(0, .75rem, 0)" }, { opacity: 1, transform: "translate3d(0, 0, 0)" }];
      }

      const animation = element.animate(frames, options);
      animation.addEventListener("finish", () => animation.cancel(), { once: true });
      observer.unobserve(element);
    }
  }, { threshold: 0.18, rootMargin: "0px 0px -4% 0px" });

  for (const target of targets) {
    target.element.motion = target;
    observer.observe(target.element);
  }

  reduceMotion.addEventListener("change", (event) => {
    if (!event.matches) return;
    observer.disconnect();
    for (const animation of document.getAnimations()) animation.cancel();
  });
})();
