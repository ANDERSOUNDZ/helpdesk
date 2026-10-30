export const environment = {
  production: false,
  useGateway: false,
  // Rutas relativas compatibles con el proxy en desarrollo y con el API Gateway en producción
  endpoints: {
    tickets: '/tickets',
    estadisticas: '/resumen',
    usuarios: '/usuarios',
    comentarios: '/comentarios',
    notificaciones: '/notificaciones'
  }
};
