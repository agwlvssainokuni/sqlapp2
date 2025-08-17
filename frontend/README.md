# SqlApp2 Frontend

React-based frontend for SqlApp2, an enterprise-grade web-based SQL execution tool.

## Technology Stack

- **React**: 19.1.1
- **TypeScript**: 5.9.2
- **Vite**: 7.1.2
- **React Router**: 7.8.0
- **Internationalization**: react-i18next v15.6.1
- **Build Tool**: Vite with Hot Module Replacement (HMR)

## Features

### Core Features
- **Multi-Language Support**: English and Japanese with 590+ translation keys
- **Visual Query Builder**: Drag-and-drop SQL query construction with real-time generation
- **Database Connection Management**: Secure connection testing and management
- **SQL Execution**: Direct SQL execution with parameterized queries and result display
- **Query Management**: Save, share, and manage SQL queries with comprehensive history
- **Schema Browser**: Database table and column information display
- **Admin Interface**: User approval workflow and email template management

### Architecture Features
- **Modular CSS**: 8-file component-specific CSS architecture for improved maintainability
- **Enhanced Authentication**: JWT token management with proactive refresh and session preservation
- **Type Safety**: Strict TypeScript configuration with comprehensive type checking
- **Responsive Design**: Mobile-first approach with CSS Grid/Flexbox
- **Performance Optimization**: Type-only imports, useCallback optimization, zero ESLint warnings

## Development Setup

### Prerequisites
- Node.js 18+ (including npm)
- Backend server running on `http://localhost:8080`

### Quick Start

```bash
# Install dependencies
npm install

# Start development server with hot reload
npm run dev

# Access frontend
# http://localhost:5173
```

### Available Scripts

```bash
# Development
npm run dev          # Start dev server with HMR
npm run build        # Build for production
npm run preview      # Preview production build
npm run lint         # Run ESLint
npm test            # Run tests

# Type checking
npm run type-check   # TypeScript type checking
```

## Project Structure

```
frontend/src/
├── components/        # Page components (ending with "Page.tsx")
│   ├── DashboardPage.tsx
│   ├── SqlExecutionPage.tsx
│   ├── QueryBuilderPage.tsx
│   ├── ConnectionManagementPage.tsx
│   ├── QueryHistoryPage.tsx
│   ├── SavedQueriesPage.tsx
│   ├── SchemaViewerPage.tsx
│   └── AdminPage.tsx
├── context/          # React context (AuthContext)
├── locales/          # i18n translation files
│   ├── en/           # English translations
│   └── ja/           # Japanese translations
├── styles/           # Modular CSS architecture
│   ├── common.css    # Layout, authentication, pagination
│   ├── Dashboard.css
│   ├── SqlExecution.css
│   ├── QueryBuilder.css
│   ├── ConnectionManagement.css
│   ├── SchemaViewer.css
│   ├── QueryHistory.css
│   └── SavedQueries.css
├── utils/            # Utility functions and API helpers
│   ├── api.ts        # Enhanced JWT token management
│   └── jwtUtils.ts   # Comprehensive token validation
└── i18n.ts           # Internationalization configuration
```

## Key Implementation Details

### Authentication System
- **JWT Token Management**: Proactive refresh (30s buffer), double-refresh prevention
- **Session Preservation**: Automatic redirect to original page after login
- **Token Validation**: Client-side JWT expiry checking with comprehensive error handling
- **Secure Storage**: localStorage with automatic cleanup on authentication failure

### Visual Query Builder
- **Complete JOIN Support**: INNER, LEFT, RIGHT, FULL OUTER joins with drag-and-drop interface
- **Bidirectional Alias Synchronization**: FROM/JOIN alias changes auto-update across all SQL clauses
- **Real-time Conflict Detection**: Duplicate alias warnings with multi-language support
- **Advanced SQL Reverse Engineering**: Complex WHERE clause parsing with BETWEEN/OR/AND operator support
- **Aggregate Function Support**: COUNT, SUM, AVG, MAX, MIN functions across SELECT, HAVING, ORDER BY clauses

### Internationalization
- **Real-time Language Switching**: Seamless transition between English and Japanese
- **Context-aware Messaging**: Dynamic translations based on application state
- **590+ Translation Keys**: Comprehensive coverage for all UI elements
- **Language Preference Storage**: User language preferences linked with backend

### Modular CSS Architecture
- **Component Isolation**: 8-file structure for improved maintainability
- **Responsive Design**: Mobile-first approach with consistent spacing
- **Theme Consistency**: Unified color scheme and typography
- **Performance**: Optimized CSS loading with component-specific styles

## API Integration

### Authentication Endpoints
- `POST /api/auth/register` - User registration with language parameter
- `POST /api/auth/login` - Login with JWT token acquisition
- `POST /api/auth/refresh` - Token refresh
- `POST /api/auth/logout` - Logout
- `GET /api/auth/me` - Current user information

### Database Management
- `GET /api/connections` - List user connections
- `POST /api/connections` - Create new connection
- `PUT /api/connections/{id}` - Update connection
- `DELETE /api/connections/{id}` - Delete connection
- `POST /api/connections/{id}/test` - Test connection

### SQL Execution
- `POST /api/sql/execute` - Execute SQL query
- `POST /api/sql/validate` - Validate SQL syntax

### Query Management
- `GET /api/queries/saved` - List saved queries
- `POST /api/queries/save` - Save query
- `GET /api/queries/history` - Execution history with date range filtering

### Admin Functions
- `GET /api/admin/users` - List users for approval
- `POST /api/admin/users/{id}/approve` - Approve user
- `POST /api/admin/users/{id}/reject` - Reject user
- `GET /api/admin/email-templates` - List email templates
- `POST /api/admin/email-templates` - Create email template

## Development Guidelines

### Code Standards
- **TypeScript Strict Mode**: Comprehensive type safety with no `any` types
- **Component Naming**: Page components end with "Page" suffix
- **API Integration**: Use `.data` property from ApiResponse structure
- **State Management**: React Context for global state, local state for UI
- **Performance**: Use `useCallback` for functions, `useRef` for parameter state management

### CSS Guidelines
- **Naming Convention**: kebab-case classes with BEM methodology
- **Responsive Design**: Mobile-first with breakpoints at 768px, 1024px
- **Component Isolation**: One CSS file per major component
- **Color Scheme**: Consistent primary (#007bff), success (#28a745), danger (#dc3545) colors

### Testing
- **Unit Tests**: Component testing with React Testing Library and vitest
- **Integration Tests**: Critical user workflows and API integrations
- **Accessibility**: ARIA labels and keyboard navigation support
- **Performance**: Bundle size monitoring and optimization

## Build and Deployment

### Production Build
```bash
npm run build
```

The build creates optimized static files in the `dist/` directory that are automatically integrated with the Spring Boot backend for unified deployment.

### Integration with Backend
The frontend is built and integrated into the Spring Boot application as static resources, providing a single WAR deployment for production environments.

### Performance Optimization
- **Code Splitting**: Automatic route-based code splitting
- **Tree Shaking**: Unused code elimination
- **Bundle Analysis**: Size optimization and dependency analysis
- **Lazy Loading**: Component-level lazy loading for improved initial load times

## Contributing

1. Follow the established component structure and naming conventions
2. Maintain TypeScript strict typing throughout
3. Add appropriate translations for new UI elements
4. Update component-specific CSS files for styling changes
5. Ensure responsive design compatibility
6. Add unit tests for new components and utilities

For detailed contribution guidelines, see the main project [CONTRIBUTING.md](../CONTRIBUTING.md).

---

**Frontend Status**: Production Ready - Enterprise-grade React application with comprehensive feature set and optimized architecture.