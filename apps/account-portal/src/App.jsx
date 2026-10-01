import { useState } from 'react'

const apiConfigured = Boolean(import.meta.env.VITE_ACCOUNT_API_URL)

function App() {
  const [mode, setMode] = useState('register')

  return (
    <main className="page-shell">
      <section className="hero">
        <p className="eyebrow">LINEAGE2 NEWERA</p>
        <h1>Seu mundo começa aqui.</h1>
        <p className="hero-copy">
          Crie e gerencie sua conta do servidor com segurança. O portal não
          acessa o banco do jogo diretamente.
        </p>
        <div className="status-pill">
          <span className={apiConfigured ? 'status-dot online' : 'status-dot'} />
          {apiConfigured ? 'API configurada' : 'Portal em preparação'}
        </div>
      </section>

      <section className="card" aria-labelledby="account-title">
        <div className="tabs" role="tablist" aria-label="Conta">
          <button
            className={mode === 'register' ? 'tab active' : 'tab'}
            onClick={() => setMode('register')}
            role="tab"
            aria-selected={mode === 'register'}
          >
            Criar conta
          </button>
          <button
            className={mode === 'login' ? 'tab active' : 'tab'}
            onClick={() => setMode('login')}
            role="tab"
            aria-selected={mode === 'login'}
          >
            Entrar
          </button>
        </div>

        <h2 id="account-title">
          {mode === 'register' ? 'Crie sua conta' : 'Acesse sua conta'}
        </h2>
        <p className="muted">
          {mode === 'register'
            ? 'O cadastro será conectado à Account API na próxima entrega.'
            : 'O login web usará uma sessão própria, separada do protocolo do jogo.'}
        </p>

        <form onSubmit={(event) => event.preventDefault()}>
          {mode === 'register' && (
            <label>
              E-mail
              <input type="email" placeholder="voce@exemplo.com" autoComplete="email" disabled />
            </label>
          )}
          <label>
            Login
            <input type="text" placeholder="Seu login" autoComplete="username" disabled />
          </label>
          <label>
            Senha
            <input type="password" placeholder="Mínimo de 8 caracteres" autoComplete={mode === 'register' ? 'new-password' : 'current-password'} disabled />
          </label>
          <button className="primary-button" type="submit" disabled>
            {mode === 'register' ? 'Cadastrar' : 'Entrar'}
          </button>
        </form>

        <p className="security-note">
          O formulário ficará ativo quando a API pública segura estiver
          disponível. Nenhum segredo do servidor será enviado ao navegador.
        </p>
      </section>
    </main>
  )
}

export default App
