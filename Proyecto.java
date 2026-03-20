/*
 * Proyecto.java
 * Sistema CRUD de gestión de tareas para consola (SENA - Taller práctico)
 *
 * Características:
 * - Clase Tarea con id, título, descripción, estado y prioridad
 * - Operaciones CRUD (crear, listar, editar, eliminar)
 * - Menú interactivo en consola
 * - Persistencia en archivo binario usando Serializable
 * - Filtrado por estado o prioridad
 * - Validaciones básicas de entrada y manejo de errores
 *
 * NOTA IMPORTANTE sobre ejecución:
 * Este programa NO requiere librerías externas. Solo necesita Java.
 *
 * Compilar:
 *    javac Proyecto.java
 *
 * Ejecutar:
 *    java Proyecto
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class Proyecto {

    // Nombre del archivo donde se guardan las tareas en formato JSON
    private static final String DATA_FILE = "tareas.json";

    // Lista en memoria que guarda las tareas durante la ejecución
    private List<Tarea> tareas;

    // Contador simple para generar IDs únicos
    private int siguienteId = 1;

    // Scanner para leer la entrada del usuario desde consola
    private final Scanner scanner;

    // Constructor: inicializa componentes y carga datos previos (si existen)
    public Proyecto() {
        this.scanner = new Scanner(System.in);
        this.tareas = new ArrayList<>();
        cargarDatos();
        ajustarSiguienteId();
    }

    /* Clase que representa una tarea. Está definida como static para facilitar
       la serialización. Todos los campos son privados y se exponen
       mediante getters/setters sencillos. */
    static class Tarea implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private int id;
        private String titulo;
        private String descripcion;
        private Estado estado;
        private Prioridad prioridad;

        // Constructor vacío (no es necesario para Serializable pero lo dejamos)
        public Tarea() {}

        public Tarea(int id, String titulo, String descripcion, Estado estado, Prioridad prioridad) {
            this.id = id;
            this.titulo = titulo;
            this.descripcion = descripcion;
            this.estado = estado;
            this.prioridad = prioridad;
        }

        public int getId() { return id; }
        public String getTitulo() { return titulo; }
        public String getDescripcion() { return descripcion; }
        public Estado getEstado() { return estado; }
        public Prioridad getPrioridad() { return prioridad; }

        public void setTitulo(String titulo) { this.titulo = titulo; }
        public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
        public void setEstado(Estado estado) { this.estado = estado; }
        public void setPrioridad(Prioridad prioridad) { this.prioridad = prioridad; }

        @Override
        public String toString() {
            return String.format("[%d] %s - %s (Estado: %s, Prioridad: %s)",
                    id, titulo, descripcion == null || descripcion.isEmpty() ? "(sin descripción)" : descripcion,
                    estado, prioridad);
        }
    }

    // Enumeraciones para limitar valores válidos
    enum Estado { PENDIENTE, EN_PROGRESO, COMPLETADA }
    enum Prioridad { BAJA, MEDIA, ALTA }

    // Lee el archivo binario de tareas (si existe) y carga la lista
    private void cargarDatos() {
        File f = new File(DATA_FILE);
        if (!f.exists()) {
            this.tareas = new ArrayList<>();
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            @SuppressWarnings("unchecked")
            List<Tarea> lista = (List<Tarea>) ois.readObject();
            this.tareas = (lista != null) ? lista : new ArrayList<>();
        } catch (IOException e) {
            System.err.println("Error al leer el archivo de datos: " + e.getMessage());
            this.tareas = new ArrayList<>();
        } catch (ClassNotFoundException e) {
            System.err.println("Error: formato de archivo incompatible: " + e.getMessage());
            this.tareas = new ArrayList<>();
        }
    }

    // Persiste la lista actual de tareas en el archivo binario
    private void guardarDatos() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(this.tareas);
        } catch (IOException e) {
            System.err.println("Error al guardar los datos: " + e.getMessage());
        }
    }

    // Calcula el siguiente id libre a partir de las tareas cargadas
    private void ajustarSiguienteId() {
        int max = 0;
        for (Tarea t : tareas) if (t.getId() > max) max = t.getId();
        siguienteId = max + 1;
    }

    // Muestra el menú principal
    private void mostrarMenu() {
        System.out.println("\n--- GESTIÓN DE TAREAS (SENA - Taller) ---");
        System.out.println("1. Crear tarea");
        System.out.println("2. Listar tareas");
        System.out.println("3. Editar tarea");
        System.out.println("4. Eliminar tarea");
        System.out.println("5. Filtrar tareas");
        System.out.println("6. Salir");
        System.out.print("Seleccione una opción: ");
    }

    // Crear nueva tarea con validaciones básicas
    private void crearTarea() {
        System.out.println("\n--- Crear nueva tarea ---");
        String titulo = leerTextoNoVacio("Título: ");
        String descripcion = leerTextoOpcional("Descripción (opcional): ");
        Estado estado = seleccionarEstado("Estado inicial");
        Prioridad prioridad = seleccionarPrioridad("Prioridad");

        Tarea t = new Tarea(siguienteId++, titulo, descripcion, estado, prioridad);
        tareas.add(t);
        guardarDatos();
        System.out.println("Tarea creada con ID " + t.getId());
    }

    // Lista todas las tareas en memoria
    private void listarTareas() {
        System.out.println("\n--- Lista de tareas ---");
        if (tareas.isEmpty()) {
            System.out.println("No hay tareas registradas.");
            return;
        }
        for (Tarea t : tareas) System.out.println(t.toString());
    }

    // Edita una tarea seleccionada por su id
    private void editarTarea() {
        System.out.println("\n--- Editar tarea ---");
        int id = leerEnteroPositivo("ID de la tarea a editar: ");
        Optional<Tarea> opt = buscarPorId(id);
        if (!opt.isPresent()) {
            System.out.println("Tarea con ID " + id + " no encontrada.");
            return;
        }
        Tarea t = opt.get();
        System.out.println("Editando: " + t.toString());

        // Se permite presionar Enter para mantener el valor actual
        String nuevoTitulo = leerTextoNoVacioConDefault("Nuevo título (enter = sin cambios): ", t.getTitulo());
        String nuevaDescripcion = leerTextoConDefault("Nueva descripción (enter = sin cambios): ", t.getDescripcion());
        Estado nuevoEstado = seleccionarEstadoConDefault("Nuevo estado", t.getEstado());
        Prioridad nuevaPrioridad = seleccionarPrioridadConDefault("Nueva prioridad", t.getPrioridad());

        t.setTitulo(nuevoTitulo);
        t.setDescripcion(nuevaDescripcion);
        t.setEstado(nuevoEstado);
        t.setPrioridad(nuevaPrioridad);

        guardarDatos();
        System.out.println("Tarea actualizada.");
    }

    // Eliminar una tarea por id (pide confirmación)
    private void eliminarTarea() {
        System.out.println("\n--- Eliminar tarea ---");
        int id = leerEnteroPositivo("ID de la tarea a eliminar: ");
        Optional<Tarea> opt = buscarPorId(id);
        if (!opt.isPresent()) {
            System.out.println("Tarea con ID " + id + " no encontrada.");
            return;
        }
        System.out.print("¿Confirmar eliminación de la tarea ID " + id + "? (s/N): ");
        String resp = scanner.nextLine().trim().toLowerCase();
        if (resp.equals("s") || resp.equals("si") || resp.equals("y") || resp.equals("yes")) {
            tareas.remove(opt.get());
            guardarDatos();
            System.out.println("Tarea eliminada.");
        } else {
            System.out.println("Eliminación cancelada.");
        }
    }

    // Permite filtrar la lista por estado o prioridad
    private void filtrarTareas() {
        System.out.println("\n--- Filtrar tareas ---");
        System.out.println("1. Filtrar por estado");
        System.out.println("2. Filtrar por prioridad");
        System.out.print("Opción: ");
        String opcion = scanner.nextLine().trim();
        if (opcion.equals("1")) {
            Estado e = seleccionarEstado("Estado a filtrar");
            List<Tarea> res = new ArrayList<>();
            for (Tarea t : tareas) if (t.getEstado() == e) res.add(t);
            mostrarListadoFiltrado(res);
        } else if (opcion.equals("2")) {
            Prioridad p = seleccionarPrioridad("Prioridad a filtrar");
            List<Tarea> res = new ArrayList<>();
            for (Tarea t : tareas) if (t.getPrioridad() == p) res.add(t);
            mostrarListadoFiltrado(res);
        } else {
            System.out.println("Opción inválida.");
        }
    }

    // Muestra lista resultado de un filtro
    private void mostrarListadoFiltrado(List<Tarea> list) {
        if (list.isEmpty()) {
            System.out.println("No se encontraron tareas con ese criterio.");
            return;
        }
        System.out.println("Tareas encontradas:");
        for (Tarea t : list) System.out.println(t.toString());
    }

    // Buscar una tarea por su id
    private Optional<Tarea> buscarPorId(int id) {
        for (Tarea t : tareas) if (t.getId() == id) return Optional.of(t);
        return Optional.empty();
    }

    /* ----- Métodos auxiliares de entrada / validaciones ----- */

    // Lee una línea que no puede estar vacía
    private String leerTextoNoVacio(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            if (!line.isEmpty()) return line;
            System.out.println("Entrada inválida: no puede estar vacío.");
        }
    }

    // Texto opcional (puede estar vacío)
    private String leerTextoOpcional(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    // Si el usuario presiona Enter, devuelve el valor por defecto
    private String leerTextoNoVacioConDefault(String prompt, String defecto) {
        System.out.print(prompt);
        String line = scanner.nextLine();
        if (line == null || line.trim().isEmpty()) return defecto;
        return line.trim();
    }

    private String leerTextoConDefault(String prompt, String defecto) {
        System.out.print(prompt);
        String line = scanner.nextLine();
        if (line == null || line.trim().isEmpty()) return defecto;
        return line.trim();
    }

    // Lee un entero positivo
    private int leerEnteroPositivo(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                int val = Integer.parseInt(line);
                if (val > 0) return val;
                System.out.println("Por favor ingrese un número entero positivo.");
            } catch (NumberFormatException e) {
                System.out.println("Entrada inválida: introduzca un número entero.");
            }
        }
    }

    // Mostrar y seleccionar un estado válido
    private Estado seleccionarEstado(String prompt) {
        while (true) {
            System.out.println(prompt + ":");
            for (int i = 0; i < Estado.values().length; i++) {
                System.out.printf("%d. %s\n", i + 1, Estado.values()[i]);
            }
            System.out.print("Seleccione opción: ");
            String line = scanner.nextLine().trim();
            try {
                int opt = Integer.parseInt(line);
                if (opt >= 1 && opt <= Estado.values().length) return Estado.values()[opt - 1];
            } catch (NumberFormatException ignored) {}
            System.out.println("Opción inválida, intente de nuevo.");
        }
    }

    // Selección con opción a mantener el valor actual (enter = sin cambios)
    private Estado seleccionarEstadoConDefault(String prompt, Estado defecto) {
        while (true) {
            System.out.println(prompt + " (enter = sin cambios):");
            for (int i = 0; i < Estado.values().length; i++) {
                System.out.printf("%d. %s\n", i + 1, Estado.values()[i]);
            }
            System.out.print("Seleccione opción: ");
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) return defecto;
            try {
                int opt = Integer.parseInt(line);
                if (opt >= 1 && opt <= Estado.values().length) return Estado.values()[opt - 1];
            } catch (NumberFormatException ignored) {}
            System.out.println("Opción inválida, intente de nuevo.");
        }
    }

    private Prioridad seleccionarPrioridad(String prompt) {
        while (true) {
            System.out.println(prompt + ":");
            for (int i = 0; i < Prioridad.values().length; i++) {
                System.out.printf("%d. %s\n", i + 1, Prioridad.values()[i]);
            }
            System.out.print("Seleccione opción: ");
            String line = scanner.nextLine().trim();
            try {
                int opt = Integer.parseInt(line);
                if (opt >= 1 && opt <= Prioridad.values().length) return Prioridad.values()[opt - 1];
            } catch (NumberFormatException ignored) {}
            System.out.println("Opción inválida, intente de nuevo.");
        }
    }

    private Prioridad seleccionarPrioridadConDefault(String prompt, Prioridad defecto) {
        while (true) {
            System.out.println(prompt + " (enter = sin cambios):");
            for (int i = 0; i < Prioridad.values().length; i++) {
                System.out.printf("%d. %s\n", i + 1, Prioridad.values()[i]);
            }
            System.out.print("Seleccione opción: ");
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) return defecto;
            try {
                int opt = Integer.parseInt(line);
                if (opt >= 1 && opt <= Prioridad.values().length) return Prioridad.values()[opt - 1];
            } catch (NumberFormatException ignored) {}
            System.out.println("Opción inválida, intente de nuevo.");
        }
    }

    // Bucle principal del programa. Se ejecuta hasta que el usuario elija salir.
    private void ejecutar() {
        try {
            while (true) {
                mostrarMenu();
                String opcion = scanner.nextLine().trim();
                switch (opcion) {
                    case "1": crearTarea(); break;
                    case "2": listarTareas(); break;
                    case "3": editarTarea(); break;
                    case "4": eliminarTarea(); break;
                    case "5": filtrarTareas(); break;
                    case "6": System.out.println("Saliendo. Guardando datos..."); guardarDatos(); return;
                    default: System.out.println("Opción inválida. Intente nuevamente.");
                }
            }
        } finally {
            // Nos aseguramos de cerrar el Scanner para liberar recursos
            // (no interfiere con System.in en aplicaciones pequeñas, pero es buena práctica)
            try {
                scanner.close();
            } catch (Exception ignored) {}
        }
    }

    // Punto de entrada. Incluimos manejo de errores para dar mensajes útiles ante errores inesperados.
    public static void main(String[] args) {
        try {
            Proyecto app = new Proyecto();
            System.out.println("Archivo de datos: " + new File(DATA_FILE).getAbsolutePath());
            app.ejecutar();
        } catch (Throwable t) {
            // Cualquier error imprevisto
            System.err.println("Ha ocurrido un error inesperado: " + t.getMessage());
            t.printStackTrace();
        }
    }
}
