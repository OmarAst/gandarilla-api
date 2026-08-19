document.addEventListener("DOMContentLoaded", () => {
  const tabs = document.querySelectorAll("[data-tab]");
  const contents = document.querySelectorAll(".tab-content");
  tabs.forEach((tab) =>
    tab.addEventListener("click", () => {
      tabs.forEach((item) => item.classList.remove("active"));
      contents.forEach((item) => item.classList.remove("active"));
      tab.classList.add("active");
      document.getElementById(tab.dataset.tab)?.classList.add("active");
    }),
  );

  const checkboxes = [
    ...document.querySelectorAll('.house-card input[type="checkbox"]'),
  ];
  const count = document.getElementById("selected-count");
  const updateCount = () => {
    if (count)
      count.textContent = checkboxes.filter((item) => item.checked).length;
  };
  checkboxes.forEach((item) => item.addEventListener("change", updateCount));
  document.getElementById("select-all")?.addEventListener("click", () => {
    checkboxes
      .filter((item) => !item.closest(".house-card").hidden)
      .forEach((item) => (item.checked = true));
    updateCount();
  });
  document.getElementById("clear-all")?.addEventListener("click", () => {
    checkboxes.forEach((item) => (item.checked = false));
    updateCount();
  });
  document
    .getElementById("house-search")
    ?.addEventListener("input", (event) => {
      const query = event.target.value.trim().toLocaleLowerCase("es");
      document.querySelectorAll(".house-card").forEach((card) => {
        card.hidden =
          query && !card.dataset.search.toLocaleLowerCase("es").includes(query);
      });
    });

  document.getElementById("batch")?.addEventListener("submit", (event) => {
    if (!checkboxes.some((item) => item.checked)) {
      event.preventDefault();
      window.alert("Selecciona al menos una casa.");
    }
  });
});
